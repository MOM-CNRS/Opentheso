package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.io.skos.ThesaurusEditionSkosImportEngine;
import fr.cnrs.opentheso.v2.concept.io.rdf.parser.ReadRdf4jDocument;
import fr.cnrs.opentheso.v2.toolbox.edition.session.ThesaurusEditionSkosImportSupport;
import lombok.RequiredArgsConstructor;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeThesaurusEditionSkosImportSupport implements ThesaurusEditionSkosImportSupport {

    private final ThesaurusEditionSkosImportEngine thesaurusEditionSkosImportEngine;

    @Override
    public SKOSXmlDocument readSkos(InputStream inputStream, RDFFormat format, String lang, StringBuffer errorBuffer)
            throws IOException {
        return new ReadRdf4jDocument().readRdfFlux(inputStream, format, lang, errorBuffer);
    }

    @Override
    public String importNewThesaurus(
            SKOSXmlDocument document,
            String formatDate,
            int userId,
            Integer projectGroupId,
            String sourceLang,
            String selectedIdentifier,
            String prefixHandle,
            String prefixDoi,
            Preferences preferences
    ) throws SQLException {
        int groupId = projectGroupId == null ? -1 : projectGroupId;
        thesaurusEditionSkosImportEngine.setInfos(formatDate, userId, groupId, sourceLang);
        thesaurusEditionSkosImportEngine.setSelectedIdentifier(selectedIdentifier);
        thesaurusEditionSkosImportEngine.setPrefixHandle(prefixHandle);
        thesaurusEditionSkosImportEngine.setPrefixDoi(prefixDoi);
        thesaurusEditionSkosImportEngine.setNodePreference(preferences);
        thesaurusEditionSkosImportEngine.setRdf4jThesaurus(document);

        String thesaurusId = thesaurusEditionSkosImportEngine.addThesaurus();
        if (thesaurusId == null) {
            return null;
        }

        var concepts = document.getConceptList();
        if (concepts != null) {
            for (SKOSResource resource : concepts) {
                if (!resource.getLabelsList().isEmpty()) {
                    thesaurusEditionSkosImportEngine.addConceptV2(resource, thesaurusId);
                }
            }
        }

        var facets = document.getFacetList();
        if (facets != null) {
            thesaurusEditionSkosImportEngine.addFacetsV2(new ArrayList<>(facets), thesaurusId);
        }

        var groups = document.getGroupList();
        if (groups != null) {
            thesaurusEditionSkosImportEngine.addGroups(new ArrayList<>(groups), thesaurusId);
        }

        thesaurusEditionSkosImportEngine.addLangsToThesaurus(thesaurusId);

        var foafImages = document.getFoafImage();
        if (foafImages != null) {
            thesaurusEditionSkosImportEngine.addFoafImages(new ArrayList<>(foafImages), thesaurusId);
        }

        return thesaurusId;
    }

    @Override
    public String getLastErrorMessage() {
        return thesaurusEditionSkosImportEngine.getMessage() == null ? "" : thesaurusEditionSkosImportEngine.getMessage().toString();
    }
}
