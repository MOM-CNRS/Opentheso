package fr.cnrs.opentheso.v2.concept.io.rdf;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatSkosImportMetadataPersistence;
import fr.cnrs.opentheso.v2.toolbox.edition.io.skos.ThesaurusEditionSkosImportEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ConceptSkosImportPersistence {

    private final ThesaurusEditionSkosImportEngine thesaurusEditionSkosImportEngine;
    private final CandidatSkosImportMetadataPersistence candidatSkosImportMetadataPersistence;

    public void configureImport(String dateFormat, int userId, int groupId, String sourceLang, Preferences preferences) {
        thesaurusEditionSkosImportEngine.setInfos(dateFormat, userId, groupId, sourceLang);
        thesaurusEditionSkosImportEngine.setNodePreference(preferences);
    }

    public void setImportDocument(SKOSXmlDocument document) {
        thesaurusEditionSkosImportEngine.setRdf4jThesaurus(document);
    }

    public void importConcept(SKOSResource resource, String thesaurusId, boolean asCandidate) throws IOException {
        thesaurusEditionSkosImportEngine.importConcept(resource, thesaurusId, asCandidate);
        if (asCandidate) {
            String conceptId = thesaurusEditionSkosImportEngine.resolveConceptId(resource);
            candidatSkosImportMetadataPersistence.saveInitialStatus(
                    conceptId,
                    thesaurusId,
                    thesaurusEditionSkosImportEngine.getIdUser()
            );
        }
    }
}
