package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.skos.exports.SkosConceptExportOperations;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Export SKOS des candidats en attente.
 * <p>
 * Utilise {@link SkosConceptExportOperations} (même chemin que le legacy
 * {@code ExportRdf4jHelperNew#exportConceptV2}) car les candidats ont le statut {@code CA}
 * et sont exclus des requêtes d'export thesaurus génériques.
 */
@Service
@RequiredArgsConstructor
public class CandidatExportService {

    private final SkosConceptExportOperations skosConceptExportOperations;

    public ExportResult exportPendingCandidates(
            String thesaurusId,
            List<CandidatDto> candidates,
            String formatCode,
            IntConsumer progressConsumer
    ) throws IOException {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalStateException("Thésaurus non sélectionné");
        }
        if (CollectionUtils.isEmpty(candidates)) {
            throw new IllegalStateException("Aucun candidat à exporter");
        }

        var preferences = skosConceptExportOperations.findThesaurusPreferences(thesaurusId)
                .orElseThrow(() -> new IllegalStateException("Préférences du thésaurus introuvables"));

        skosConceptExportOperations.prepareExport(preferences);

        var skosDocument = new SKOSXmlDocument();
        skosDocument.setConceptScheme(skosConceptExportOperations.exportThesaurusScheme(thesaurusId, preferences));

        int total = candidates.size();
        int index = 0;
        for (CandidatDto candidat : candidates) {
            index++;
            if (progressConsumer != null) {
                progressConsumer.accept(index * 100 / total);
            }
            if (candidat == null || StringUtils.isBlank(candidat.getIdConcepte())) {
                continue;
            }
            // true = export candidat (statut CA + métadonnées messages/votes)
            SKOSResource resource = skosConceptExportOperations.exportConcept(
                    thesaurusId, candidat.getIdConcepte(), true);
            if (resource != null && StringUtils.isNotBlank(resource.getUri())) {
                skosDocument.addconcept(resource);
            }
        }

        if (CollectionUtils.isEmpty(skosDocument.getConceptList())
                || skosDocument.getConceptList().stream().noneMatch(Objects::nonNull)) {
            throw new IllegalStateException("Aucun candidat exportable");
        }

        var format = resolveFormat(formatCode);
        byte[] content = skosConceptExportOperations.serializeSkos(skosDocument, format.rdfFormat());
        return new ExportResult(content, "candidats" + format.extension(), contentType(format.rdfFormat()));
    }

    private ResolvedFormat resolveFormat(String formatCode) {
        return switch (formatCode == null ? "" : formatCode.toLowerCase()) {
            case "jsonld" -> new ResolvedFormat(RDFFormat.JSONLD, ".json");
            case "turtle" -> new ResolvedFormat(RDFFormat.TURTLE, ".ttl");
            case "json" -> new ResolvedFormat(RDFFormat.RDFJSON, ".json");
            default -> new ResolvedFormat(RDFFormat.RDFXML, ".rdf");
        };
    }

    private static String contentType(RDFFormat format) {
        if (RDFFormat.TURTLE.equals(format)) {
            return "text/turtle";
        }
        if (RDFFormat.JSONLD.equals(format) || RDFFormat.RDFJSON.equals(format)) {
            return "application/json";
        }
        return "application/rdf+xml";
    }

    private record ResolvedFormat(RDFFormat rdfFormat, String extension) {
    }

    public record ExportResult(byte[] content, String filename, String contentType) {
    }
}
