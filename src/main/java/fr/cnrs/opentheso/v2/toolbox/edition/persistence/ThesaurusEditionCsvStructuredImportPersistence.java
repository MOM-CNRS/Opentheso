package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptCreationWriteRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.io.csv.ThesaurusCsvImportEngine;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptLabel;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredImportResult;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusEditionStructuredParseResult;
import fr.cnrs.opentheso.v2.toolbox.edition.support.ThesaurusImportBatchSupport;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredImportPersistence {

    private static final String FORMAT_DATE = "yyyy-MM-dd";

    private final ThesaurusCsvImportEngine thesaurusCsvImportEngine;
    private final ToolboxPreferencePersistence toolboxPreferencePersistence;
    private final ConceptCreationWriteRepository conceptCreationWriteRepository;
    private final ThesaurusImportBatchSupport importBatchSupport;

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
            toolboxPreferencePersistence.initPreferences(id, normalizedSourceLang);
            toolboxPreferencePersistence.updatePreferredName(id, resolvedName);
            thesaurusCsvImportEngine.setFormatDate(FORMAT_DATE);
            thesaurusCsvImportEngine.setIdUser(userId);
            thesaurusCsvImportEngine.setNodePreference(toolboxPreferencePersistence.findPreferences(id));
            return id;
        });

        if (StringUtils.isBlank(thesaurusId)) {
            return ThesaurusEditionStructuredImportResult.error("Erreur lors de la création du thésaurus");
        }

        int conceptCount = countConcepts(root);
        List<Long> reservedIds = conceptCreationWriteRepository.reserveNumericConceptIds(conceptCount);
        Iterator<Long> idIterator = reservedIds.iterator();

        List<ThesaurusCsvConceptObject> concepts = new ArrayList<>(conceptCount);
        for (NodeTree child : root.getChildrens()) {
            flattenTree(child, null, normalizedSourceLang, concepts, idIterator);
        }

        AtomicInteger importedConcepts = new AtomicInteger();
        String finalThesaurusId = thesaurusId;
        importBatchSupport.forEachBatched(concepts, (batch, ignored) -> {
            for (ThesaurusCsvConceptObject concept : batch) {
                if (thesaurusCsvImportEngine.addConceptV2(finalThesaurusId, concept, userId, FORMAT_DATE)) {
                    importedConcepts.incrementAndGet();
                }
            }
        });

        toolboxPreferencePersistence.updateLastSyncAt(thesaurusId, java.time.LocalDateTime.now());

        return new ThesaurusEditionStructuredImportResult(thesaurusId, importedConcepts.get(), null);
    }

    private int countConcepts(NodeTree root) {
        int count = 0;
        for (NodeTree child : root.getChildrens()) {
            count += countNode(child);
        }
        return count;
    }

    private int countNode(NodeTree node) {
        if (node == null || StringUtils.isBlank(node.getPreferredTerm())) {
            return 0;
        }
        int count = 1;
        if (CollectionUtils.isNotEmpty(node.getChildrens())) {
            for (NodeTree child : node.getChildrens()) {
                count += countNode(child);
            }
        }
        return count;
    }

    private void flattenTree(
            NodeTree nodeTree,
            String parentConceptId,
            String sourceLang,
            List<ThesaurusCsvConceptObject> concepts,
            Iterator<Long> idIterator
    ) {
        if (nodeTree == null || StringUtils.isBlank(nodeTree.getPreferredTerm())) {
            return;
        }
        if (!idIterator.hasNext()) {
            throw new IllegalStateException("Réservation d'identifiants insuffisante pour l'import structuré");
        }

        String conceptId = String.valueOf(idIterator.next());
        nodeTree.setIdConcept(conceptId);

        var concept = new ThesaurusCsvConceptObject();
        concept.setIdConcept(conceptId);
        concept.setType("skos:Concept");
        concept.setConceptType("concept");

        var prefLabel = new ThesaurusCsvConceptLabel();
        prefLabel.setLabel(nodeTree.getPreferredTerm().trim());
        prefLabel.setLang(sourceLang);
        concept.getPrefLabels().add(prefLabel);

        if (StringUtils.isNotBlank(parentConceptId)) {
            concept.getBroaders().add(parentConceptId);
        }

        concepts.add(concept);

        if (CollectionUtils.isNotEmpty(nodeTree.getChildrens())) {
            for (NodeTree child : nodeTree.getChildrens()) {
                flattenTree(child, conceptId, sourceLang, concepts, idIterator);
            }
        }
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
