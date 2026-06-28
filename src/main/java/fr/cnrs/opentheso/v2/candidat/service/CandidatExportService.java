package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.exports.rdf4j.ExportRdf4jHelperNew;
import fr.cnrs.opentheso.services.exports.rdf4j.WriteRdf4j;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.IntConsumer;

@Service
@RequiredArgsConstructor
public class CandidatExportService {

    private final ExportRdf4jHelperNew exportRdf4jHelperNew;
    private final PreferenceService preferenceService;

    public ExportResult exportPendingCandidates(
            String thesaurusId,
            List<CandidatDto> candidates,
            String formatCode,
            IntConsumer progressConsumer
    ) throws IOException {
        if (CollectionUtils.isEmpty(candidates)) {
            throw new IllegalStateException("Aucun candidat à exporter");
        }

        var preferences = preferenceService.getThesaurusPreferences(thesaurusId);
        if (preferences == null) {
            throw new IllegalStateException("Préférences du thésaurus introuvables");
        }

        var skosDocument = buildSkosDocument(thesaurusId, candidates, progressConsumer);
        var format = resolveFormat(formatCode);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Rio.write(new WriteRdf4j(skosDocument).getModel(), out, format.rdfFormat());

        return new ExportResult(out.toByteArray(), "candidats" + format.extension(), "application/xml");
    }

    private SKOSXmlDocument buildSkosDocument(
            String thesaurusId,
            List<CandidatDto> candidates,
            IntConsumer progressConsumer
    ) {
        var skosXmlDocument = new SKOSXmlDocument();
        var preferences = preferenceService.getThesaurusPreferences(thesaurusId);
        skosXmlDocument.setConceptScheme(exportRdf4jHelperNew.exportThesoV2(thesaurusId, preferences));

        int step = candidates.isEmpty() ? 0 : 100 / candidates.size();
        int progress = 0;
        for (CandidatDto candidat : candidates) {
            progress += step;
            if (progressConsumer != null) {
                progressConsumer.accept(progress);
            }
            skosXmlDocument.addconcept(
                    exportRdf4jHelperNew.exportConceptV2(thesaurusId, candidat.getIdConcepte(), true)
            );
        }
        return skosXmlDocument;
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
