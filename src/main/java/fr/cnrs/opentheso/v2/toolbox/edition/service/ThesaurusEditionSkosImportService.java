package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.v2.concept.io.rdf.parser.ReadRdf4jDocument;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.toolbox.edition.io.skos.ThesaurusEditionSkosImportEngine;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionSkosImportService {

    private final ThesaurusEditionSkosImportEngine thesaurusEditionSkosImportEngine;
    private final NewThesaurusService newThesaurusService;

    public SkosLoadResult loadSkosFile(
            InputStream inputStream,
            int typeImport,
            String selectedLang,
            StringBuffer errorBuffer
    ) throws IOException {
        String lang = StringUtils.isBlank(selectedLang) ? "fr" : selectedLang;
        var document = new ReadRdf4jDocument().readRdfFlux(
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

    @Transactional
    public String importNewThesaurus(
            SKOSXmlDocument document,
            String formatDate,
            int userId,
            boolean superAdmin,
            Integer projectGroupId,
            String sourceLang,
            String selectedIdentifier,
            String prefixHandle,
            String prefixDoi
    ) throws SQLException {
        var preferences = new Preferences();
        preferences.setSourceLang(StringUtils.defaultIfBlank(sourceLang, "fr"));

        Integer groupId = projectGroupId;
        if (!superAdmin && groupId == null) {
            NewThesaurusFormOptions options = newThesaurusService.loadFormOptions(userId, false);
            if (options.projects().size() == 1) {
                groupId = options.projects().get(0).id();
            }
        }

        String thesaurusId = importSkosDocument(
                document,
                StringUtils.defaultIfBlank(formatDate, "yyyy-MM-dd"),
                userId,
                groupId,
                StringUtils.defaultIfBlank(sourceLang, "fr"),
                StringUtils.defaultIfBlank(selectedIdentifier, "sans"),
                StringUtils.defaultIfBlank(prefixHandle, ""),
                StringUtils.defaultIfBlank(prefixDoi, ""),
                preferences
        );

        if (thesaurusId == null) {
            throw new IllegalStateException(getLastErrorMessage());
        }
        return thesaurusId;
    }

    private String importSkosDocument(
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

    private String getLastErrorMessage() {
        return thesaurusEditionSkosImportEngine.getMessage() == null
                ? ""
                : thesaurusEditionSkosImportEngine.getMessage().toString();
    }

    public record SkosLoadResult(SKOSXmlDocument document, String uri, int totalConcepts) {
    }
}
