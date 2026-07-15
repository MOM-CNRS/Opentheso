package fr.cnrs.opentheso.v2.concept.io.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.ConceptSkosRdfImportEngine;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class ConceptSkosImportService {

    private final ConceptSkosRdfImportEngine conceptSkosRdfImportEngine;
    private final PreferencesRepository preferencesRepository;

    public SkosLoadResult loadSkosFile(
            InputStream inputStream,
            int typeImport,
            String selectedLang,
            StringBuffer errorBuffer
    ) throws IOException {
        String lang = StringUtils.isBlank(selectedLang) ? "fr" : selectedLang;
        var document = conceptSkosRdfImportEngine.readSkos(
                inputStream,
                SkosRdfFormatSupport.resolveImportFormat(typeImport),
                lang,
                errorBuffer
        );
        return new SkosLoadResult(
                document,
                document.getTitle(),
                document.getConceptList() == null ? 0 : document.getConceptList().size()
        );
    }

    public void importConceptsForThesaurus(
            SKOSXmlDocument document,
            String thesaurusId,
            int userId,
            String sourceLang,
            ProgressCallback progressCallback
    ) throws IOException {
        var preferences = preferencesRepository.findByIdThesaurus(thesaurusId)
                .orElseThrow(() -> new IOException("Préférences du thésaurus introuvables"));
        importConcepts(document, thesaurusId, userId, -1, sourceLang, preferences, progressCallback);
    }

    public void importConcepts(
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
            if (!resource.getLabelsList().isEmpty()) {
                conceptSkosRdfImportEngine.importConcept(resource, thesaurusId);
            }
        }
    }

    public record SkosLoadResult(SKOSXmlDocument document, String uri, int totalConcepts) {
    }

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int current, int total);
    }
}
