package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.RelationGroupService;
import fr.cnrs.opentheso.services.imports.csv.CsvImportHelper;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvReader;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvImportOperations {

    private final CsvImportHelper csvImportHelper;
    private final PreferenceService preferenceService;
    private final RelationGroupService relationGroupService;

    public CsvParseResult parse(byte[] content, char delimiter) {
        if (content == null || content.length == 0) {
            return CsvParseResult.error("Fichier CSV vide");
        }

        var csvReader = new ThesaurusCsvReader(delimiter);
        try (var reader1 = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            if (!csvReader.setLangs(reader1)) {
                return CsvParseResult.error(csvReader.getMessage());
            }
        } catch (Exception ex) {
            return CsvParseResult.error(ex.getMessage());
        }

        try (var reader2 = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8)) {
            if (!csvReader.readFile(reader2, false)) {
                return CsvParseResult.error(csvReader.getMessage());
            }
        } catch (Exception ex) {
            return CsvParseResult.error(ex.getMessage());
        }

        var conceptObjects = csvReader.getConceptObjects();
        if (CollectionUtils.isEmpty(conceptObjects)) {
            return CsvParseResult.error("Aucune donnée CSV détectée");
        }
        if (conceptObjects.get(0).getPrefLabels() == null || conceptObjects.get(0).getPrefLabels().isEmpty()) {
            return CsvParseResult.error("La lecture a échoué, vérifiez le séparateur des colonnes");
        }

        return new CsvParseResult(
                conceptObjects,
                csvReader.getLangs(),
                conceptObjects.size(),
                csvReader.getMessage(),
                null
        );
    }

    public CsvImportResult importNewThesaurus(CsvImportCommand command) {
        if (CollectionUtils.isEmpty(command.conceptObjects())) {
            return CsvImportResult.error("Aucun concept à importer");
        }

        int projectId = command.projectGroupId() == null ? -1 : command.projectGroupId();
        String sourceLang = StringUtils.defaultIfBlank(command.sourceLang(), "fr");

        var nodeUser = new NodeUser();
        nodeUser.setIdUser(command.userId());
        nodeUser.setName(command.userName());

        String thesaurusId = csvImportHelper.createThesaurus(
                StringUtils.defaultString(command.thesaurusName()),
                sourceLang,
                projectId,
                nodeUser
        );
        if (StringUtils.isBlank(thesaurusId)) {
            return CsvImportResult.error("Erreur lors de la création du thésaurus");
        }

        if (CollectionUtils.isNotEmpty(command.languages())) {
            csvImportHelper.addLangsToThesaurus(command.languages(), thesaurusId);
        }

        preferenceService.initPreferences(thesaurusId, sourceLang);
        csvImportHelper.setNodePreference(preferenceService.getThesaurusPreferences(thesaurusId));
        csvImportHelper.setFormatDate(StringUtils.defaultIfBlank(command.formatDate(), "yyyy-MM-dd"));

        int importedConcepts = 0;
        var legacyConceptObjects = ThesaurusCsvConceptObjectBridge.toLegacyList(command.conceptObjects());
        for (var conceptObject : legacyConceptObjects) {
            switch (StringUtils.defaultString(conceptObject.getType()).trim().toLowerCase()) {
                case "skos:concept" -> {
                    if (csvImportHelper.addConceptV2(
                            thesaurusId,
                            conceptObject,
                            command.userId(),
                            csvImportHelper.getFormatDate()
                    )) {
                        importedConcepts++;
                    }
                }
                case "skos:collection" -> {
                    csvImportHelper.addGroup(thesaurusId, conceptObject);
                    for (String subGroup : conceptObject.getSubGroups()) {
                        relationGroupService.addSubGroup(conceptObject.getIdConcept(), subGroup, thesaurusId);
                    }
                }
                case "skos-thes:thesaurusarray" -> csvImportHelper.addFacets(conceptObject, thesaurusId);
                default -> {
                    // ignore unknown types
                }
            }
        }

        return new CsvImportResult(
                thesaurusId,
                importedConcepts,
                StringUtils.defaultString(csvImportHelper.getMessage())
        );
    }

    public record CsvParseResult(
            List<ThesaurusCsvConceptObject> conceptObjects,
            List<String> languages,
            int totalConcepts,
            String warning,
            String error
    ) {
        public static CsvParseResult error(String message) {
            return new CsvParseResult(List.of(), List.of(), 0, null, message);
        }

        public boolean isSuccess() {
            return StringUtils.isBlank(error);
        }
    }

    public record CsvImportCommand(
            String thesaurusName,
            String sourceLang,
            String formatDate,
            Integer projectGroupId,
            int userId,
            String userName,
            List<ThesaurusCsvConceptObject> conceptObjects,
            List<String> languages
    ) {
    }

    public record CsvImportResult(String thesaurusId, int importedConcepts, String message) {
        public static CsvImportResult error(String message) {
            return new CsvImportResult(null, 0, message);
        }

        public boolean isSuccess() {
            return StringUtils.isNotBlank(thesaurusId);
        }
    }
}
