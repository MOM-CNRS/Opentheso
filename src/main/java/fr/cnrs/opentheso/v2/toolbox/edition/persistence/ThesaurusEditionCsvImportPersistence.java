package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvImportEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvReader;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionCsvParseResult;
import fr.cnrs.opentheso.v2.toolbox.edition.support.ThesaurusImportBatchSupport;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvImportPersistence {

    private final ThesaurusCsvImportEngine thesaurusCsvImportEngine;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ThesaurusImportBatchSupport importBatchSupport;

    public ThesaurusEditionCsvParseResult parse(byte[] content, char delimiter) {
        if (content == null || content.length == 0) {
            return ThesaurusEditionCsvParseResult.error("Fichier CSV vide");
        }

        var csvReader = new ThesaurusCsvReader(delimiter);
        try (var reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            if (!csvReader.setLangs(reader)) {
                return ThesaurusEditionCsvParseResult.error(csvReader.getMessage());
            }
            // Relire depuis le début du même buffer pour le corps (évite un second décodage du flux source)
            reader.close();
        } catch (Exception ex) {
            return ThesaurusEditionCsvParseResult.error(ex.getMessage());
        }

        try (var reader2 = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            if (!csvReader.readFile(reader2, false)) {
                return ThesaurusEditionCsvParseResult.error(csvReader.getMessage());
            }
        } catch (Exception ex) {
            return ThesaurusEditionCsvParseResult.error(ex.getMessage());
        }

        var conceptObjects = csvReader.getConceptObjects();
        if (CollectionUtils.isEmpty(conceptObjects)) {
            return ThesaurusEditionCsvParseResult.error("Aucune donnée CSV détectée");
        }
        if (conceptObjects.get(0).getPrefLabels() == null || conceptObjects.get(0).getPrefLabels().isEmpty()) {
            return ThesaurusEditionCsvParseResult.error("La lecture a échoué, vérifiez le séparateur des colonnes");
        }

        return new ThesaurusEditionCsvParseResult(
                conceptObjects,
                csvReader.getLangs(),
                conceptObjects.size(),
                csvReader.getMessage(),
                null
        );
    }

    public ThesaurusEditionCsvImportResult importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            String formatDate,
            Integer projectGroupId,
            int userId,
            String userName,
            List<ThesaurusCsvConceptObject> conceptObjects,
            List<String> languages
    ) {
        if (CollectionUtils.isEmpty(conceptObjects)) {
            return ThesaurusEditionCsvImportResult.error("Aucun concept à importer");
        }

        int projectId = projectGroupId == null ? -1 : projectGroupId;
        String normalizedSourceLang = StringUtils.defaultIfBlank(sourceLang, "fr");
        String normalizedFormatDate = StringUtils.defaultIfBlank(formatDate, "yyyy-MM-dd");

        String thesaurusId = importBatchSupport.inTransaction(() -> {
            String id = thesaurusCsvImportEngine.createThesaurus(
                    StringUtils.defaultString(thesaurusName),
                    normalizedSourceLang,
                    projectId,
                    userName
            );
            if (StringUtils.isBlank(id)) {
                return null;
            }
            String resolvedName = StringUtils.defaultIfBlank(thesaurusName, "theso_" + id);
            if (CollectionUtils.isNotEmpty(languages)) {
                thesaurusCsvImportEngine.addLangsToThesaurus(languages, id);
            }
            toolboxPreferencePersistence.initPreferences(id, normalizedSourceLang);
            toolboxPreferencePersistence.updatePreferredName(id, resolvedName);
            thesaurusCsvImportEngine.setNodePreference(toolboxPreferencePersistence.findPreferences(id));
            thesaurusCsvImportEngine.setFormatDate(normalizedFormatDate);
            thesaurusCsvImportEngine.setIdUser(userId);
            return id;
        });

        if (StringUtils.isBlank(thesaurusId)) {
            return ThesaurusEditionCsvImportResult.error("Erreur lors de la création du thésaurus");
        }

        AtomicInteger importedConcepts = new AtomicInteger();
        String finalThesaurusId = thesaurusId;
        importBatchSupport.forEachBatched(conceptObjects, (batch, ignored) -> {
            for (var conceptObject : batch) {
                switch (StringUtils.defaultString(conceptObject.getType()).trim().toLowerCase()) {
                    case "skos:concept" -> {
                        if (thesaurusCsvImportEngine.addConceptV2(
                                finalThesaurusId,
                                conceptObject,
                                userId,
                                normalizedFormatDate
                        )) {
                            importedConcepts.incrementAndGet();
                        }
                    }
                    case "skos:collection" -> {
                        thesaurusCsvImportEngine.addGroup(finalThesaurusId, conceptObject);
                        for (String subGroup : conceptObject.getSubGroups()) {
                            thesaurusCsvImportEngine.addSubGroup(conceptObject.getIdConcept(), subGroup, finalThesaurusId);
                        }
                    }
                    case "skos-thes:thesaurusarray" -> thesaurusCsvImportEngine.addFacets(conceptObject, finalThesaurusId);
                    default -> {
                        // ignore unknown types
                    }
                }
            }
        });

        toolboxPreferencePersistence.updateLastSyncAt(thesaurusId, java.time.LocalDateTime.now());

        return new ThesaurusEditionCsvImportResult(
                thesaurusId,
                importedConcepts.get(),
                StringUtils.defaultString(thesaurusCsvImportEngine.getMessage())
        );
    }
}
