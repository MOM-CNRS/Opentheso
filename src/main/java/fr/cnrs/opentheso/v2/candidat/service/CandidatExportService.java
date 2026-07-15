package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.export.rdf.ConceptSkosExportPersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.function.IntConsumer;

@Service
@RequiredArgsConstructor
public class CandidatExportService {

    private final ConceptSkosExportPersistence conceptSkosExportPersistence;

    public ExportResult exportPendingCandidates(
            String thesaurusId,
            List<CandidatDto> candidates,
            String formatCode,
            IntConsumer progressConsumer
    ) throws IOException {
        if (CollectionUtils.isEmpty(candidates)) {
            throw new IllegalStateException("Aucun candidat à exporter");
        }

        var preferences = conceptSkosExportPersistence.findThesaurusPreferences(thesaurusId).orElse(null);
        if (preferences == null) {
            throw new IllegalStateException("Préférences du thésaurus introuvables");
        }

        var skosDocument = buildSkosDocument(thesaurusId, candidates, progressConsumer);
        var format = resolveFormat(formatCode);
        byte[] content = conceptSkosExportPersistence.serializeSkos(skosDocument, format.rdfFormat());

        return new ExportResult(content, "candidats" + format.extension(), "application/xml");
    }

    private SKOSXmlDocument buildSkosDocument(
            String thesaurusId,
            List<CandidatDto> candidates,
            IntConsumer progressConsumer
    ) {
        var skosXmlDocument = new SKOSXmlDocument();
        var preferences = conceptSkosExportPersistence.findThesaurusPreferences(thesaurusId).orElse(null);
        skosXmlDocument.setConceptScheme(exportConceptScheme(thesaurusId, preferences));

        int step = candidates.isEmpty() ? 0 : 100 / candidates.size();
        int progress = 0;
        for (CandidatDto candidat : candidates) {
            progress += step;
            if (progressConsumer != null) {
                progressConsumer.accept(progress);
            }
            skosXmlDocument.addconcept(exportConcept(thesaurusId, candidat.getIdConcepte(), true));
        }
        return skosXmlDocument;
    }

    private SKOSResource exportConceptScheme(String thesaurusId, Preferences preferences) {
        return conceptSkosExportPersistence.exportConceptScheme(thesaurusId, preferences);
    }

    private SKOSResource exportConcept(String thesaurusId, String conceptId, boolean includeRelations) {
        try {
            return conceptSkosExportPersistence.exportConcept(thesaurusId, conceptId, includeRelations);
        } catch (Exception ex) {
            throw new IllegalStateException("Export SKOS candidat impossible", ex);
        }
    }

    private ResolvedFormat resolveFormat(String formatCode) {
        return switch (formatCode == null ? "" : formatCode.toLowerCase()) {
            case "jsonld" -> new ResolvedFormat(RDFFormat.JSONLD, ".json");
            case "turtle" -> new ResolvedFormat(RDFFormat.TURTLE, ".ttl");
            case "json" -> new ResolvedFormat(RDFFormat.RDFJSON, ".json");
            default -> new ResolvedFormat(RDFFormat.RDFXML, ".rdf");
        };
    }

    private record ResolvedFormat(RDFFormat rdfFormat, String extension) {
    }

    public record ExportResult(byte[] content, String filename, String contentType) {
    }
}
