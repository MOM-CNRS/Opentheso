package fr.cnrs.opentheso.edition.imports;

import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.imports.csv.CsvImportHelper;
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
public class ThesaurusEditionCsvStructuredImportOperations {

    private final CsvImportHelper csvImportHelper;
    private final PreferenceService preferenceService;
    private final ConceptAddService conceptAddService;

    public StructuredParseResult parse(byte[] content, char delimiter) {
        if (content == null || content.length == 0) {
            return StructuredParseResult.error("Fichier CSV vide");
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
            return StructuredParseResult.error(ex.getMessage());
        }

        if (lines.isEmpty()) {
            return StructuredParseResult.error("Aucune donnée CSV détectée");
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
            return StructuredParseResult.error("Aucun concept détecté dans le fichier");
        }

        return new StructuredParseResult(root, total, null);
    }

    public StructuredImportResult importNewThesaurus(StructuredImportCommand command) {
        if (command.root() == null || CollectionUtils.isEmpty(command.root().getChildrens())) {
            return StructuredImportResult.error("Aucun concept à importer");
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
            return StructuredImportResult.error("Erreur lors de la création du thésaurus");
        }

        preferenceService.initPreferences(thesaurusId, sourceLang);

        int importedConcepts = 0;
        for (NodeTree child : command.root().getChildrens()) {
            importedConcepts += insertTree(child, thesaurusId, null, sourceLang, command.userId());
        }

        return new StructuredImportResult(thesaurusId, importedConcepts, null);
    }

    private int insertTree(NodeTree nodeTree, String thesaurusId, String parentConceptId, String sourceLang, int userId) {
        var concept = new Concept();
        concept.setIdThesaurus(thesaurusId);
        concept.setStatus("D");
        concept.setIdConcept(null);
        concept.setTopConcept(false);

        var term = new Term();
        term.setIdThesaurus(thesaurusId);
        term.setLang(sourceLang);
        term.setLexicalValue(nodeTree.getPreferredTerm().trim());
        term.setSource("");
        term.setStatus("D");

        String conceptId = conceptAddService.addConcept(parentConceptId, "NT", concept, term, userId);
        int imported = 1;
        if (CollectionUtils.isNotEmpty(nodeTree.getChildrens())) {
            for (NodeTree child : nodeTree.getChildrens()) {
                imported += insertTree(child, thesaurusId, conceptId, sourceLang, userId);
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

    public record StructuredParseResult(NodeTree root, int totalConcepts, String error) {
        public static StructuredParseResult error(String message) {
            return new StructuredParseResult(null, 0, message);
        }

        public boolean isSuccess() {
            return StringUtils.isBlank(error);
        }
    }

    public record StructuredImportCommand(
            String thesaurusName,
            String sourceLang,
            Integer projectGroupId,
            int userId,
            String userName,
            NodeTree root
    ) {
    }

    public record StructuredImportResult(String thesaurusId, int importedConcepts, String message) {
        public static StructuredImportResult error(String message) {
            return new StructuredImportResult(null, 0, message);
        }

        public boolean isSuccess() {
            return StringUtils.isNotBlank(thesaurusId);
        }
    }
}
