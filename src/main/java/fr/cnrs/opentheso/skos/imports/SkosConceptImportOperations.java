package fr.cnrs.opentheso.skos.imports;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.services.imports.rdf4j.ImportRdf4jHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SkosConceptImportOperations {

    private final ImportRdf4jHelper importRdf4jHelper;

    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        importRdf4jHelper.setInfos(dateFormat, userId, groupId, sourceLang);
        importRdf4jHelper.setNodePreference(preferences);
    }

    public void setImportDocument(SKOSXmlDocument document) {
        importRdf4jHelper.setRdf4jThesaurus(document);
    }

    public void importConcept(SKOSResource resource, String thesaurusId) throws IOException {
        importRdf4jHelper.addConcept(resource, thesaurusId, false);
    }

    public void importCandidateConcept(SKOSResource resource, String thesaurusId) throws IOException {
        importRdf4jHelper.addConcept(resource, thesaurusId, true);
    }
}
