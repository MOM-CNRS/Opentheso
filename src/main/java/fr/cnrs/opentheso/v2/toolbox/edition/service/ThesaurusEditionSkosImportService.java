package fr.cnrs.opentheso.v2.toolbox.edition.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.models.nodes.DcElement;
import fr.cnrs.opentheso.models.skosapi.SKOSLabel;
import fr.cnrs.opentheso.models.skosapi.SKOSResource;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.ThesaurusLabelRepository;
import fr.cnrs.opentheso.v2.concept.io.rdf.parser.ReadRdf4jDocument;
import fr.cnrs.opentheso.v2.shared.io.SkosRdfFormatSupport;
import fr.cnrs.opentheso.v2.toolbox.edition.io.skos.ThesaurusEditionSkosImportEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.support.ThesaurusImportBatchSupport;
import fr.cnrs.opentheso.v2.toolbox.model.NewThesaurusFormOptions;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ThesaurusEditionSkosImportService {

    private final ThesaurusEditionSkosImportEngine thesaurusEditionSkosImportEngine;
    private final NewThesaurusService newThesaurusService;
    private final ThesaurusImportBatchSupport importBatchSupport;
    private final ThesaurusLabelRepository thesaurusLabelRepository;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;

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

    /**
     * Détecte si un thésaurus portant le même titre existe déjà (dans le projet, ou globalement).
     */
    public Optional<String> findExistingThesaurusId(
            SKOSXmlDocument document,
            Integer projectGroupId,
            String sourceLang
    ) {
        String title = resolveConceptSchemeTitle(document, sourceLang);
        if (StringUtils.isBlank(title)) {
            return Optional.empty();
        }
        List<String> matches;
        if (projectGroupId != null && projectGroupId > 0) {
            matches = thesaurusLabelRepository.findThesaurusIdsByProjectAndTitle(projectGroupId, title);
        } else {
            matches = thesaurusLabelRepository.findThesaurusIdsByTitle(title);
        }
        if (matches == null || matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    public String resolveConceptSchemeTitle(SKOSXmlDocument document, String sourceLang) {
        if (document == null || document.getConceptScheme() == null) {
            return null;
        }
        SKOSResource conceptScheme = document.getConceptScheme();
        String lang = StringUtils.defaultIfBlank(sourceLang, "fr");
        if (conceptScheme.getLabelsList() != null) {
            for (SKOSLabel label : conceptScheme.getLabelsList()) {
                if (StringUtils.isNotBlank(label.getLabel())
                        && lang.equalsIgnoreCase(StringUtils.defaultString(label.getLanguage()))) {
                    return label.getLabel().trim();
                }
            }
            for (SKOSLabel label : conceptScheme.getLabelsList()) {
                if (StringUtils.isNotBlank(label.getLabel())) {
                    return label.getLabel().trim();
                }
            }
        }
        if (conceptScheme.getThesaurus() != null) {
            if (StringUtils.isNotBlank(conceptScheme.getThesaurus().getTitle())) {
                return conceptScheme.getThesaurus().getTitle().trim();
            }
            if (conceptScheme.getThesaurus().getDcElement() != null) {
                for (DcElement dcElement : conceptScheme.getThesaurus().getDcElement()) {
                    if ("title".equalsIgnoreCase(dcElement.getName()) && StringUtils.isNotBlank(dcElement.getValue())) {
                        return dcElement.getValue().trim();
                    }
                }
            }
        }
        return null;
    }

    public String importNewThesaurus(
            SKOSXmlDocument document,
            String formatDate,
            int userId,
            boolean superAdmin,
            Integer projectGroupId,
            String sourceLang,
            String selectedIdentifier,
            String prefixHandle,
            String prefixDoi,
            String persistentNameThesaurus
            String prefixDoi,
            boolean importAsMaster
    ) throws SQLException {
        var preferences = new Preferences();
        preferences.setSourceLang(StringUtils.defaultIfBlank(sourceLang, "fr"));
        preferences.setPreferredName(persistentNameThesaurus);

        Integer groupId = projectGroupId;
        if (!superAdmin && groupId == null) {
            NewThesaurusFormOptions options = newThesaurusService.loadFormOptions(userId, false);
            if (options.projects().size() == 1) {
                groupId = options.projects().get(0).id();
            }
        }

        // Nouveau thésaurus → toujours esclave ; existant → choix utilisateur
        boolean asMaster = false;
        if (importAsMaster) {
            Optional<String> existing = findExistingThesaurusId(document, groupId, sourceLang);
            asMaster = existing.isPresent();
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
                preferences,
                asMaster
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
            Preferences preferences,
            boolean importAsMaster
    ) throws SQLException {
        int groupId = projectGroupId == null ? -1 : projectGroupId;
        thesaurusEditionSkosImportEngine.setInfos(formatDate, userId, groupId, sourceLang);
        thesaurusEditionSkosImportEngine.setSelectedIdentifier(selectedIdentifier);
        thesaurusEditionSkosImportEngine.setPrefixHandle(prefixHandle);
        thesaurusEditionSkosImportEngine.setPrefixDoi(prefixDoi);
        thesaurusEditionSkosImportEngine.setNodePreference(preferences);
        thesaurusEditionSkosImportEngine.setImportAsMaster(importAsMaster);
        thesaurusEditionSkosImportEngine.setRdf4jThesaurus(document);

        String thesaurusId;
        try {
            thesaurusId = importBatchSupport.inTransaction(() -> {
                try {
                    return thesaurusEditionSkosImportEngine.addThesaurus();
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IllegalStateException ex) {
            if (ex.getCause() instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw ex;
        }
        if (thesaurusId == null) {
            return null;
        }

        var concepts = document.getConceptList();
        if (concepts != null && !concepts.isEmpty()) {
            List<SKOSResource> withLabels = new ArrayList<>();
            for (SKOSResource resource : concepts) {
                if (!resource.getLabelsList().isEmpty()) {
                    withLabels.add(resource);
                }
            }
            String finalThesaurusId = thesaurusId;
            importBatchSupport.forEachBatched(withLabels, (batch, ignored) -> {
                for (SKOSResource resource : batch) {
                    thesaurusEditionSkosImportEngine.addConceptV2(resource, finalThesaurusId);
                }
            });
        }

        String finalThesaurusId = thesaurusId;
        importBatchSupport.inTransaction(() -> {
            var facets = document.getFacetList();
            if (facets != null) {
                thesaurusEditionSkosImportEngine.addFacetsV2(new ArrayList<>(facets), finalThesaurusId);
            }

            var groups = document.getGroupList();
            if (groups != null) {
                thesaurusEditionSkosImportEngine.addGroups(new ArrayList<>(groups), finalThesaurusId);
            }

            thesaurusEditionSkosImportEngine.addLangsToThesaurus(finalThesaurusId);

            var foafImages = document.getFoafImage();
            if (foafImages != null) {
                thesaurusEditionSkosImportEngine.addFoafImages(new ArrayList<>(foafImages), finalThesaurusId);
            }
            importBatchSupport.flushAndClear();
            // Baseline sync : l'import n'est pas une modification locale à pousser vers le maître.
            toolboxPreferencePersistence.updateLastSyncAt(finalThesaurusId, LocalDateTime.now());
        });

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
