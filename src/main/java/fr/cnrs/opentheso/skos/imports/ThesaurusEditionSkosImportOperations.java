package fr.cnrs.opentheso.skos.imports;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionSkosImportOperations {

    private final ThesaurusSkosImportEngine thesaurusSkosImportEngine;

    public void configure(
            String formatDate,
            int userId,
            int projectGroupId,
            String sourceLang,
            String selectedIdentifier,
            String prefixHandle,
            String prefixDoi,
            Preferences preferences
    ) {
        thesaurusSkosImportEngine.setInfos(formatDate, userId, projectGroupId, sourceLang);
        thesaurusSkosImportEngine.setSelectedIdentifier(selectedIdentifier);
        thesaurusSkosImportEngine.setPrefixHandle(prefixHandle);
        thesaurusSkosImportEngine.setPrefixDoi(prefixDoi);
        thesaurusSkosImportEngine.setNodePreference(preferences);
    }

    public void setImportDocument(SKOSXmlDocument document) {
        thesaurusSkosImportEngine.setRdf4jThesaurus(document);
    }

    public String importNewThesaurus(SKOSXmlDocument document) throws SQLException {
        setImportDocument(document);
        String thesaurusId = thesaurusSkosImportEngine.addThesaurus();
        if (thesaurusId == null) {
            return null;
        }

        var concepts = document.getConceptList();
        if (concepts != null) {
            for (SKOSResource resource : concepts) {
                if (!resource.getLabelsList().isEmpty()) {
                    thesaurusSkosImportEngine.addConceptV2(resource, thesaurusId);
                }
            }
        }

        var facets = document.getFacetList();
        if (facets != null) {
            thesaurusSkosImportEngine.addFacetsV2(new ArrayList<>(facets), thesaurusId);
        }

        var groups = document.getGroupList();
        if (groups != null) {
            thesaurusSkosImportEngine.addGroups(new ArrayList<>(groups), thesaurusId);
        }

        thesaurusSkosImportEngine.addLangsToThesaurus(thesaurusId);

        var foafImages = document.getFoafImage();
        if (foafImages != null) {
            thesaurusSkosImportEngine.addFoafImages(new ArrayList<>(foafImages), thesaurusId);
        }

        return thesaurusId;
    }

    public void importConcept(SKOSResource resource, String thesaurusId) throws IOException {
        thesaurusSkosImportEngine.addConcept(resource, thesaurusId, false);
    }

    public String getLastErrorMessage() {
        return thesaurusSkosImportEngine.getMessage() == null ? "" : thesaurusSkosImportEngine.getMessage().toString();
    }
}
