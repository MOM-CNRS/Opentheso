package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfImportEngine;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class CandidatSkosImportService {

    private final ConceptSkosRdfImportEngine conceptSkosRdfImportEngine;
    private final ThesaurusPreferencesProvider thesaurusPreferencesProvider;

    public SkosLoadResult loadSkosFile(InputStream inputStream, int typeImport, String selectedLang, StringBuilder errorBuffer) throws IOException {
        String lang = StringUtils.isBlank(selectedLang) ? "fr" : selectedLang;
        var document = conceptSkosRdfImportEngine.readSkos(inputStream, resolveFormat(typeImport), lang, errorBuffer);
        return new SkosLoadResult(
                document,
                document.getTitle(),
                document.getConceptList() == null ? 0 : document.getConceptList().size()
        );
    }

    public void importCandidatesForThesaurus(
            SKOSXmlDocument document,
            String thesaurusId,
            int userId,
            String sourceLang,
            ProgressCallback progressCallback
    ) throws IOException {
        var preferences = thesaurusPreferencesProvider.findPreferences(thesaurusId)
                .orElseThrow(() -> new IOException("Préférences du thésaurus introuvables"));
        importCandidates(document, thesaurusId, userId, -1, sourceLang, preferences, progressCallback);
    }

    public void importCandidates(
            SKOSXmlDocument document,
            String thesaurusId,
            int userId,
            int groupId,
            String sourceLang,
            Preferences preferences,
            ProgressCallback progressCallback
    ) throws IOException {
        conceptSkosRdfImportEngine.configureImport("yyyy-MM-dd", userId, groupId, sourceLang, preferences);
        conceptSkosRdfImportEngine.setImportDocument(document);

        var concepts = document.getConceptList();
        if (concepts == null || concepts.isEmpty()) {
            return;
        }

        int index = 0;
        for (SKOSResource resource : concepts) {
            index++;
            if (progressCallback != null) {
                progressCallback.onProgress(index, concepts.size());
            }
            conceptSkosRdfImportEngine.importConcept(resource, thesaurusId, true);
        }
    }

    private RDFFormat resolveFormat(int typeImport) {
        return switch (typeImport) {
            case 1 -> RDFFormat.JSONLD;
            case 2 -> RDFFormat.TURTLE;
            case 3 -> RDFFormat.RDFJSON;
            default -> RDFFormat.RDFXML;
        };
    }

    public record SkosLoadResult(SKOSXmlDocument document, String uri, int totalConcepts) {
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int current, int total);
    }
}
