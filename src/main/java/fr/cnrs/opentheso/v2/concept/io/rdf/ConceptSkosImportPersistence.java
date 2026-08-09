package fr.cnrs.opentheso.v2.concept.io.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.skos.imports.SkosConceptImportOperations;
import fr.cnrs.opentheso.v2.toolbox.edition.io.skos.ThesaurusEditionSkosImportEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ConceptSkosImportPersistence {

    private final ThesaurusEditionSkosImportEngine thesaurusEditionSkosImportEngine;
    private final SkosConceptImportOperations skosConceptImportOperations;

    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        thesaurusEditionSkosImportEngine.setInfos(dateFormat, userId, groupId, sourceLang);
        thesaurusEditionSkosImportEngine.setNodePreference(preferences);
        // Même chemin que l'import candidat legacy (insertConcept, pas la procédure SQL).
        skosConceptImportOperations.configureImport(dateFormat, userId, groupId, sourceLang, preferences);
    }

    public void setImportDocument(SKOSXmlDocument document) {
        thesaurusEditionSkosImportEngine.setRdf4jThesaurus(document);
        skosConceptImportOperations.setImportDocument(document);
    }

    public void importConcept(SKOSResource resource, String thesaurusId, boolean asCandidate) throws IOException {
        if (asCandidate) {
            skosConceptImportOperations.importCandidateConcept(resource, thesaurusId);
            return;
        }
        thesaurusEditionSkosImportEngine.importConcept(resource, thesaurusId, false);
    }
}
