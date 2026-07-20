package fr.cnrs.opentheso.v2.toolbox.edition.persistence;

import fr.cnrs.opentheso.models.NodeDeprecatedProjection;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.relations.NodeDeprecated;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvAlignmentRow;
import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvByIdRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ThesaurusEditionCsvExportPersistence {

    private final ConceptRepository conceptRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> listConceptIds(String thesaurusId, List<String> groupIds) {
        if (CollectionUtils.isEmpty(groupIds)) {
            @SuppressWarnings("unchecked")
            List<String> ids = entityManager.createNativeQuery("""
                            SELECT id_concept
                            FROM concept
                            WHERE id_thesaurus = :thesaurusId
                              AND status <> 'CA'
                            ORDER BY id_concept
                            """)
                    .setParameter("thesaurusId", thesaurusId)
                    .getResultList();
            return ids;
        }
        List<String> conceptIds = new ArrayList<>();
        for (String groupId : groupIds) {
            conceptIds.addAll(conceptRepository.findAllConceptIdsByGroup(thesaurusId, groupId));
        }
        return conceptIds.stream().distinct().toList();
    }

    /**
     * Charge toutes les lignes CSV-by-id en quelques requêtes (au lieu de 5 × N).
     */
    public List<ThesaurusCsvByIdRow> loadConceptsForCsvById(String thesaurusId, String languageCode, List<String> conceptIds) {
        if (CollectionUtils.isEmpty(conceptIds)) {
            return List.of();
        }

        Map<String, ConceptIds> concepts = new HashMap<>();
        Map<String, String> prefLabels = new HashMap<>();
        Map<String, List<String>> altLabels = new HashMap<>();
        Map<String, List<String>> definitions = new HashMap<>();
        Map<String, List<ThesaurusCsvAlignmentRow>> alignments = new HashMap<>();

        for (List<String> chunk : partition(conceptIds, 1000)) {
            concepts.putAll(loadConceptIdentifiers(thesaurusId, chunk));
            prefLabels.putAll(loadPrefLabels(thesaurusId, languageCode, chunk));
            mergeListMap(altLabels, loadAltLabels(thesaurusId, languageCode, chunk));
            mergeListMap(definitions, loadDefinitions(thesaurusId, languageCode, chunk));
            mergeListMap(alignments, loadAlignments(thesaurusId, chunk));
        }

        List<ThesaurusCsvByIdRow> rows = new ArrayList<>(conceptIds.size());
        for (String conceptId : conceptIds) {
            ConceptIds ids = concepts.get(conceptId);
            if (ids == null) {
                continue;
            }
            rows.add(new ThesaurusCsvByIdRow(
                    conceptId,
                    ids.arkId(),
                    ids.handleId(),
                    prefLabels.getOrDefault(conceptId, ""),
                    altLabels.getOrDefault(conceptId, List.of()),
                    definitions.getOrDefault(conceptId, List.of()),
                    alignments.getOrDefault(conceptId, List.of())
            ));
        }
        return rows;
    }

    public List<NodeDeprecated> listDeprecatedConcepts(String thesaurusId, String languageCode) {
        List<NodeDeprecated> deprecatedList = new ArrayList<>();
        List<NodeDeprecatedProjection> projections = conceptRepository.findAllDeprecatedConcepts(thesaurusId, languageCode);
        if (projections.isEmpty()) {
            return deprecatedList;
        }

        List<String> deprecatedIds = projections.stream().map(NodeDeprecatedProjection::getIdConcept).toList();
        Map<String, List<NodeIdValue>> replacedByMap = loadReplacedByBatch(thesaurusId, languageCode, deprecatedIds);

        for (NodeDeprecatedProjection projection : projections) {
            NodeDeprecated node = new NodeDeprecated();
            node.setDeprecatedId(projection.getIdConcept());
            node.setDeprecatedLabel(projection.getLexicalValue());
            node.setModified(projection.getModified());
            node.setUserName(projection.getUsername());

            List<NodeIdValue> replacesValues = replacedByMap.getOrDefault(projection.getIdConcept(), List.of());
            if (!replacesValues.isEmpty()) {
                node.setReplacedById(replacesValues.stream().map(NodeIdValue::getId).collect(Collectors.joining("##")));
                node.setReplacedByLabel(replacesValues.stream().map(NodeIdValue::getValue).collect(Collectors.joining("##")));
            }
            deprecatedList.add(node);
        }
        return deprecatedList;
    }

    private Map<String, ConceptIds> loadConceptIdentifiers(String thesaurusId, List<String> conceptIds) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT id_concept, COALESCE(id_ark, ''), COALESCE(id_handle, '')
                        FROM concept
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept IN (:conceptIds)
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptIds", conceptIds)
                .getResultList();
        Map<String, ConceptIds> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], new ConceptIds((String) row[1], (String) row[2]));
        }
        return map;
    }

    private Map<String, String> loadPrefLabels(String thesaurusId, String languageCode, List<String> conceptIds) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT pt.id_concept, t.lexical_value
                        FROM preferred_term pt
                        JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                        WHERE pt.id_thesaurus = :thesaurusId
                          AND t.lang = :lang
                          AND pt.id_concept IN (:conceptIds)
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", languageCode)
                .setParameter("conceptIds", conceptIds)
                .getResultList();
        Map<String, String> map = new HashMap<>();
        for (Object[] row : rows) {
            map.putIfAbsent((String) row[0], row[1] != null ? (String) row[1] : "");
        }
        return map;
    }

    private Map<String, List<String>> loadAltLabels(String thesaurusId, String languageCode, List<String> conceptIds) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT pt.id_concept, npt.lexical_value
                        FROM non_preferred_term npt
                        JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                        WHERE npt.id_thesaurus = :thesaurusId
                          AND npt.lang = :lang
                          AND pt.id_concept IN (:conceptIds)
                        ORDER BY npt.lexical_value
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", languageCode)
                .setParameter("conceptIds", conceptIds)
                .getResultList();
        Map<String, List<String>> map = new HashMap<>();
        for (Object[] row : rows) {
            map.computeIfAbsent((String) row[0], key -> new ArrayList<>())
                    .add(row[1] != null ? (String) row[1] : "");
        }
        return map;
    }

    private Map<String, List<String>> loadDefinitions(String thesaurusId, String languageCode, List<String> conceptIds) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT identifier, lexicalvalue
                        FROM note
                        WHERE id_thesaurus = :thesaurusId
                          AND lang = :lang
                          AND LOWER(notetypecode) = 'definition'
                          AND identifier IN (:conceptIds)
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", languageCode)
                .setParameter("conceptIds", conceptIds)
                .getResultList();
        Map<String, List<String>> map = new HashMap<>();
        for (Object[] row : rows) {
            map.computeIfAbsent((String) row[0], key -> new ArrayList<>())
                    .add(row[1] != null ? (String) row[1] : "");
        }
        return map;
    }

    private Map<String, List<ThesaurusCsvAlignmentRow>> loadAlignments(String thesaurusId, List<String> conceptIds) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT a.internal_id_concept, t.label, a.uri_target
                        FROM alignement a
                        JOIN alignement_type t ON a.alignement_id_type = t.id
                        WHERE a.internal_id_thesaurus = :thesaurusId
                          AND a.internal_id_concept IN (:conceptIds)
                        ORDER BY a.id
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptIds", conceptIds)
                .getResultList();
        Map<String, List<ThesaurusCsvAlignmentRow>> map = new HashMap<>();
        for (Object[] row : rows) {
            map.computeIfAbsent((String) row[0], key -> new ArrayList<>())
                    .add(new ThesaurusCsvAlignmentRow(
                            row[1] != null ? (String) row[1] : "",
                            row[2] != null ? (String) row[2] : ""
                    ));
        }
        return map;
    }

    private Map<String, List<NodeIdValue>> loadReplacedByBatch(String thesaurusId, String languageCode, List<String> deprecatedIds) {
        Map<String, List<NodeIdValue>> map = new LinkedHashMap<>();
        for (List<String> chunk : partition(deprecatedIds, 1000)) {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = entityManager.createNativeQuery("""
                            SELECT cr.id_concept1, cr.id_concept2, COALESCE(t.lexical_value, '')
                            FROM concept_replacedby cr
                            LEFT JOIN preferred_term pt
                                ON pt.id_concept = cr.id_concept2 AND pt.id_thesaurus = cr.id_thesaurus
                            LEFT JOIN term t
                                ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus AND t.lang = :lang
                            WHERE cr.id_thesaurus = :thesaurusId
                              AND cr.id_concept1 IN (:deprecatedIds)
                            """)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("lang", languageCode)
                    .setParameter("deprecatedIds", chunk)
                    .getResultList();
            for (Object[] row : rows) {
                map.computeIfAbsent((String) row[0], key -> new ArrayList<>())
                        .add(NodeIdValue.builder()
                                .id((String) row[1])
                                .value(StringUtils.defaultString((String) row[2]))
                                .build());
            }
        }
        return map;
    }

    private record ConceptIds(String arkId, String handleId) {
    }

    private static <T> List<List<T>> partition(List<T> source, int size) {
        List<List<T>> parts = new ArrayList<>();
        for (int i = 0; i < source.size(); i += size) {
            parts.add(source.subList(i, Math.min(i + size, source.size())));
        }
        return parts;
    }

    private static <T> void mergeListMap(Map<String, List<T>> target, Map<String, List<T>> source) {
        source.forEach((key, values) -> target.computeIfAbsent(key, ignored -> new ArrayList<>()).addAll(values));
    }
}
