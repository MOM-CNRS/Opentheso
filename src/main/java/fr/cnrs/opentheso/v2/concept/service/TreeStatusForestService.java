package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.api.dto.TreeStatusForestNode;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabelSort;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TreeStatusForestService {

    private final ConceptQueryRepository conceptQueryRepository;

    public TreeStatusForestService(ConceptQueryRepository conceptQueryRepository) {
        this.conceptQueryRepository = conceptQueryRepository;
    }

    @Transactional(readOnly = true)
    public List<TreeStatusForestNode> loadForest(String thesaurusId, String lang, Collection<String> statuses) {
        if (StringUtils.isAnyBlank(thesaurusId, lang) || statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        Set<String> selected = new HashSet<>();
        for (String status : statuses) {
            if (StringUtils.isNotBlank(status)) {
                selected.add(status.trim());
            }
        }
        if (selected.isEmpty()) {
            return List.of();
        }
        List<Object[]> matches = conceptQueryRepository.findConceptsForUiStatuses(thesaurusId, lang, selected);
        List<Seed> seeds = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Object[] row : matches) {
            Seed seed = seedFromRow(row);
            if (seed.id.isBlank()) {
                continue;
            }
            seeds.add(seed);
            ids.add(seed.id);
        }
        if (seeds.isEmpty()) {
            return List.of();
        }
        List<Edge> edges = new ArrayList<>();
        for (Object[] row : conceptQueryRepository.findAncestorEdgesForConcepts(thesaurusId, lang, ids)) {
            Edge edge = edgeFromRow(row);
            if (!edge.childId.isBlank() && !edge.parentId.isBlank()) {
                edges.add(edge);
            }
        }
        return assemble(seeds, edges, selected);
    }

    static List<TreeStatusForestNode> assemble(List<Seed> matches, List<Edge> edges, Set<String> selected) {
        Map<String, String> parentOf = new HashMap<>();
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (Seed seed : matches) {
            nodes.put(seed.id, nodeFromSeed(seed, selected));
        }
        for (Edge edge : edges) {
            parentOf.putIfAbsent(edge.childId, edge.parentId);
            nodes.computeIfAbsent(edge.parentId, id -> nodeFromParent(id, edge.parentLabel, edge.parentStatus, selected));
        }
        for (Map.Entry<String, String> link : parentOf.entrySet()) {
            Node child = nodes.get(link.getKey());
            Node parent = nodes.get(link.getValue());
            if (child == null || parent == null || child == parent) {
                continue;
            }
            if (!parent.children.contains(child)) {
                parent.children.add(child);
            }
        }
        Set<Node> children = new HashSet<>();
        for (Node node : nodes.values()) {
            children.addAll(node.children);
        }
        List<Node> roots = new ArrayList<>();
        for (Node node : nodes.values()) {
            if (!children.contains(node)) {
                roots.add(node);
            }
        }
        sortTree(roots);
        List<TreeStatusForestNode> flat = new ArrayList<>();
        for (Node root : roots) {
            flatten(root, 0, flat);
        }
        return List.copyOf(flat);
    }

    private static void sortTree(List<Node> nodes) {
        nodes.sort(Comparator.comparing((Node node) -> node.label, ConceptLabelSort::compareLabels));
        for (Node node : nodes) {
            sortTree(node.children);
        }
    }

    private static void flatten(Node node, int depth, List<TreeStatusForestNode> out) {
        node.hasChildren = !node.children.isEmpty();
        out.add(new TreeStatusForestNode(
                node.id,
                node.label,
                node.notation,
                node.status,
                nodeType(node.status),
                depth,
                node.inactive,
                node.hasChildren,
                node.candidateBy,
                node.candidateOn
        ));
        for (Node child : node.children) {
            flatten(child, depth + 1, out);
        }
    }

    private static String nodeType(String status) {
        return switch (status) {
            case "candidat" -> "candidat";
            case "rejete" -> "rejete";
            case "deprecie" -> "deprecated";
            case "insere" -> "insere";
            default -> "concept";
        };
    }

    private static Seed seedFromRow(Object[] row) {
        return new Seed(
                str(row, 0),
                str(row, 2),
                str(row, 1),
                StringUtils.defaultIfBlank(str(row, 3), "valide"),
                str(row, 4),
                str(row, 5)
        );
    }

    private static Edge edgeFromRow(Object[] row) {
        return new Edge(str(row, 0), str(row, 1), str(row, 2), StringUtils.defaultIfBlank(str(row, 3), "valide"));
    }

    private static Node nodeFromSeed(Seed seed, Set<String> selected) {
        return new Node(
                seed.id,
                StringUtils.defaultIfBlank(seed.label, seed.id),
                seed.notation,
                seed.status,
                !selected.contains(seed.status),
                seed.candidateBy,
                seed.candidateOn
        );
    }

    private static Node nodeFromParent(String id, String label, String status, Set<String> selected) {
        String ui = StringUtils.defaultIfBlank(status, "valide");
        return new Node(id, StringUtils.defaultIfBlank(label, id), "", ui, !selected.contains(ui), "", "");
    }

    private static String str(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }

    record Seed(String id, String label, String notation, String status, String candidateBy, String candidateOn) {
    }

    record Edge(String childId, String parentId, String parentLabel, String parentStatus) {
    }

    private static final class Node {
        private final String id;
        private final String label;
        private final String notation;
        private final String status;
        private final boolean inactive;
        private final String candidateBy;
        private final String candidateOn;
        private final List<Node> children = new ArrayList<>();
        private boolean hasChildren;

        private Node(
                String id,
                String label,
                String notation,
                String status,
                boolean inactive,
                String candidateBy,
                String candidateOn
        ) {
            this.id = id;
            this.label = label;
            this.notation = notation;
            this.status = status;
            this.inactive = inactive;
            this.candidateBy = candidateBy;
            this.candidateOn = candidateOn;
        }
    }
}
