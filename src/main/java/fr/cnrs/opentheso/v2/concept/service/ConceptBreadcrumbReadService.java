package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.BreadcrumbStep;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConceptBreadcrumbReadService {

    private final ConceptQueryRepository conceptQueryRepository;

    @Transactional(readOnly = true)
    public List<List<BreadcrumbStep>> loadBreadcrumbPaths(String thesaurusId, String conceptId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, conceptId, lang)) {
            return Collections.emptyList();
        }

        // One CTE query fetches the full ancestor graph (edges + labels)
        List<Object[]> ancestorRows = conceptQueryRepository.findAncestorsWithLabels(conceptId, thesaurusId, lang);

        if (ancestorRows.isEmpty()) {
            // Concept is already a top-level — single-node path
            String label = conceptQueryRepository.findConceptLabel(conceptId, thesaurusId, lang);
            BreadcrumbStep step = new BreadcrumbStep(conceptId, label, 0, true);
            return List.of(List.of(step));
        }

        // Build adjacency: child → list of parents, and label map for parent nodes
        Map<String, List<String>> childToParents = new LinkedHashMap<>();
        Map<String, String> labelByConceptId = new HashMap<>();

        for (Object[] row : ancestorRows) {
            String child  = str(row[0]);
            String parent = str(row[1]);
            String label  = str(row[3]);
            childToParents.computeIfAbsent(child, k -> new ArrayList<>()).add(parent);
            labelByConceptId.put(parent, StringUtils.defaultIfBlank(label, "(" + parent + ")"));
        }

        // Label of the leaf concept (the original requested concept)
        String leafLabel = conceptQueryRepository.findConceptLabel(conceptId, thesaurusId, lang);
        labelByConceptId.put(conceptId, leafLabel);

        // Find root nodes (parents that never appear as children)
        List<String> roots = new ArrayList<>();
        for (Object[] row : ancestorRows) {
            String parent = str(row[1]);
            if (!childToParents.containsKey(parent) && !roots.contains(parent)) {
                roots.add(parent);
            }
        }

        // Build all root-to-leaf paths by DFS from each root
        List<List<String>> allPaths = new ArrayList<>();
        for (String root : roots) {
            List<String> path = new ArrayList<>();
            path.add(root);
            buildPathsDown(root, conceptId, childToParents, path, allPaths);
        }

        if (allPaths.isEmpty()) {
            allPaths.add(List.of(conceptId));
        }

        // Convert String id paths → labeled BreadcrumbStep paths
        return allPaths.stream()
                .map(path -> labelPath(path, labelByConceptId))
                .toList();
    }

    private void buildPathsDown(
            String current,
            String target,
            Map<String, List<String>> childToParents,
            List<String> path,
            List<List<String>> results
    ) {
        if (current.equals(target)) {
            results.add(new ArrayList<>(path));
            return;
        }
        // Find children of current: nodes whose parent list contains current
        for (Map.Entry<String, List<String>> entry : childToParents.entrySet()) {
            if (entry.getValue().contains(current)) {
                String child = entry.getKey();
                if (!path.contains(child)) {
                    path.add(child);
                    buildPathsDown(child, target, childToParents, path, results);
                    path.remove(path.size() - 1);
                }
            }
        }
    }

    private List<BreadcrumbStep> labelPath(List<String> ids, Map<String, String> labels) {
        List<BreadcrumbStep> steps = new ArrayList<>();
        boolean first = true;
        for (String id : ids) {
            String label = labels.getOrDefault(id, "(" + id + ")");
            steps.add(new BreadcrumbStep(id, label, 0, first));
            first = false;
        }
        return steps;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "";
    }
}
