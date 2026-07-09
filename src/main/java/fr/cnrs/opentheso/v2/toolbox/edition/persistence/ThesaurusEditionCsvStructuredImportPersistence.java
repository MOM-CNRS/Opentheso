package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTopConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptStructureNativeWriteService;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvImportEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredParseResult;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxPreferencePersistence;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredImportPersistence {

    private final ThesaurusCsvImportEngine thesaurusCsvImportEngine;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ConceptStructureNativeWriteService conceptStructureNativeWriteService;

    public ThesaurusEditionStructuredParseResult parse(byte[] content, char delimiter) {
        if (content == null || content.length == 0) {
            return ThesaurusEditionStructuredParseResult.error("Fichier CSV vide");
        }

        List<String[]> lines = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isNotEmpty(line)) {
                    lines.add(line.split(String.valueOf(delimiter), -1));
                }
            }
        } catch (Exception ex) {
            return ThesaurusEditionStructuredParseResult.error(ex.getMessage());
        }

        if (lines.isEmpty()) {
            return ThesaurusEditionStructuredParseResult.error("Aucune donnée CSV détectée");
        }

        int maxColumns = lines.stream().mapToInt(row -> row.length).max().orElse(0);
        String[][] matrix = new String[lines.size() + 1][maxColumns + 1];
        int total = 0;
        for (int i = 0; i < lines.size(); i++) {
            for (int j = 0; j < maxColumns; j++) {
                if (lines.get(i).length > j) {
                    matrix[i][j] = lines.get(i)[j];
                    if (StringUtils.isNotEmpty(lines.get(i)[j])) {
                        total++;
                    }
                }
            }
        }

        var root = new NodeTree();
        for (int i = 0; i < matrix.length; i++) {
            if (StringUtils.isNotEmpty(matrix[i][0])) {
                root.getChildrens().add(createTree(matrix, i, 0));
            }
        }

        if (CollectionUtils.isEmpty(root.getChildrens())) {
            return ThesaurusEditionStructuredParseResult.error("Aucun concept détecté dans le fichier");
        }

        return new ThesaurusEditionStructuredParseResult(root, total, null);
    }

    public ThesaurusEditionStructuredImportResult importNewThesaurus(
            String thesaurusName,
            String sourceLang,
            Integer projectGroupId,
            int userId,
            String userName,
            NodeTree root
    ) {
        if (root == null || CollectionUtils.isEmpty(root.getChildrens())) {
            return ThesaurusEditionStructuredImportResult.error("Aucun concept à importer");
        }

        int projectId = projectGroupId == null ? -1 : projectGroupId;
        String normalizedSourceLang = StringUtils.defaultIfBlank(sourceLang, "fr");

        String thesaurusId = thesaurusCsvImportEngine.createThesaurus(
                StringUtils.defaultString(thesaurusName),
                normalizedSourceLang,
                projectId,
                userName
        );
        if (StringUtils.isBlank(thesaurusId)) {
            return ThesaurusEditionStructuredImportResult.error("Erreur lors de la création du thésaurus");
        }

        toolboxPreferencePersistence.initPreferences(thesaurusId, normalizedSourceLang);

        int importedConcepts = 0;
        for (NodeTree child : root.getChildrens()) {
            importedConcepts += insertTree(child, thesaurusId, null, normalizedSourceLang, userId, userName);
        }

        return new ThesaurusEditionStructuredImportResult(thesaurusId, importedConcepts, null);
    }

    private int insertTree(
            NodeTree nodeTree,
            String thesaurusId,
            String parentConceptId,
            String sourceLang,
            int userId,
            String userName
    ) {
        String conceptId;
        if (parentConceptId == null) {
            var result = conceptStructureNativeWriteService.addTopConcept(new AddTopConceptCommand(
                    thesaurusId,
                    sourceLang,
                    userId,
                    userName,
                    nodeTree.getPreferredTerm().trim(),
                    null,
                    null,
                    "",
                    null,
                    false
            ));
            if (!result.success() || StringUtils.isBlank(result.createdConceptId())) {
                return 0;
            }
            conceptId = result.createdConceptId();
        } else {
            var result = conceptStructureNativeWriteService.addChildConcept(new AddChildConceptCommand(
                    thesaurusId,
                    parentConceptId,
                    sourceLang,
                    userId,
                    userName,
                    nodeTree.getPreferredTerm().trim(),
                    null,
                    null,
                    "",
                    null,
                    "NT",
                    false
            ));
            if (!result.success() || StringUtils.isBlank(result.createdConceptId())) {
                return 0;
            }
            conceptId = result.createdConceptId();
        }

        int imported = 1;
        if (CollectionUtils.isNotEmpty(nodeTree.getChildrens())) {
            for (NodeTree child : nodeTree.getChildrens()) {
                imported += insertTree(child, thesaurusId, conceptId, sourceLang, userId, userName);
            }
        }
        return imported;
    }

    private NodeTree createTree(String[][] matrix, int row, int column) {
        var element = new NodeTree();
        element.setPreferredTerm(matrix[row][column]);

        column++;
        row++;
        if (row < matrix.length && column < matrix[row].length) {
            while (matrix[row][column] != null) {
                if (matrix[row][column - 1] != null
                        && matrix[row][column - 1].length() > 0
                        && !matrix[row][column - 1].equals(element.getPreferredTerm())) {
                    break;
                }
                if (matrix[row][column].length() > 0) {
                    element.getChildrens().add(createTree(matrix, row, column));
                }
                row++;
            }
        }
        return element;
    }
}
