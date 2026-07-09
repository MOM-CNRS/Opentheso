package fr.cnrs.opentheso.skos.imports;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SkosConceptImportOperations {

    private final ThesaurusSkosImportEngine thesaurusSkosImportEngine;

    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        thesaurusSkosImportEngine.setInfos(dateFormat, userId, groupId, sourceLang);
        thesaurusSkosImportEngine.setNodePreference(preferences);
    }

    public void setImportDocument(SKOSXmlDocument document) {
        thesaurusSkosImportEngine.setRdf4jThesaurus(document);
    }

    public void importConcept(SKOSResource resource, String thesaurusId) throws IOException {
        thesaurusSkosImportEngine.addConcept(resource, thesaurusId, false);
    }

    public void importCandidateConcept(SKOSResource resource, String thesaurusId) throws IOException {
        thesaurusSkosImportEngine.addConcept(resource, thesaurusId, true);
    }
}
