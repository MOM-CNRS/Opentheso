package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.services.imports.rdf4j.ImportRdf4jHelper;
import fr.cnrs.opentheso.services.imports.rdf4j.ReadRDF4JNewGen;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class CandidatSkosImportService {

    private final ImportRdf4jHelper importRdf4jHelper;

    public SkosLoadResult loadSkosFile(InputStream inputStream, int typeImport, String selectedLang, StringBuffer errorBuffer) throws IOException {
        String lang = StringUtils.isBlank(selectedLang) ? "fr" : selectedLang;
        var document = new ReadRDF4JNewGen().readRdfFlux(inputStream, resolveFormat(typeImport), lang, errorBuffer);
        return new SkosLoadResult(
                document,
                document.getTitle(),
                document.getConceptList() == null ? 0 : document.getConceptList().size()
        );
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
        importRdf4jHelper.setInfos("yyyy-MM-dd", userId, groupId, sourceLang);
        importRdf4jHelper.setNodePreference(preferences);
        importRdf4jHelper.setRdf4jThesaurus(document);

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
                importRdf4jHelper.addConcept(resource, thesaurusId, true);
            }
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
