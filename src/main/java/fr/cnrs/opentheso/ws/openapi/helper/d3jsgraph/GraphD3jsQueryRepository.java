package fr.cnrs.opentheso.ws.openapi.helper.d3jsgraph;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Chargement set-based des données nécessaires au graphe D3js,
 * pour éviter le N+1 via {@code opentheso_get_concept} / {@code getFullConcept}.
 */
@Repository
@Transactional(readOnly = true)
public class GraphD3jsQueryRepository {

    private static final int IN_CLAUSE_CHUNK = 800;

    @PersistenceContext
    private EntityManager em;

    public List<Object[]> findConceptIdsLimited(String idThesaurus, int maxConcepts) {
        return em.createNativeQuery("""
            SELECT c.id_concept, COALESCE(c.id_ark, ''), COALESCE(c.id_handle, '')
            FROM concept c
            WHERE c.id_thesaurus = :idThesaurus
              AND c.status <> 'CA'
            ORDER BY c.id_concept
            LIMIT :maxConcepts
            """)
                .setParameter("idThesaurus", idThesaurus)
                .setParameter("maxConcepts", maxConcepts)
                .getResultList();
    }

    public List<Object[]> findConceptsByIds(String idThesaurus, Collection<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(conceptIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT c.id_concept, COALESCE(c.id_ark, ''), COALESCE(c.id_handle, '')
                FROM concept c
                WHERE c.id_thesaurus = :idThesaurus
                  AND c.status <> 'CA'
                  AND c.id_concept IN (:conceptIds)
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("conceptIds", chunk)
                    .getResultList());
        }
        return result;
    }

    public List<Object[]> findPrefLabels(String idThesaurus, Collection<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(conceptIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT pt.id_concept, t.lang, t.lexical_value
                FROM preferred_term pt
                JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = pt.id_thesaurus
                WHERE pt.id_thesaurus = :idThesaurus
                  AND pt.id_concept IN (:conceptIds)
                ORDER BY pt.id_concept, t.lang
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("conceptIds", chunk)
                    .getResultList());
        }
        return result;
    }

    public List<Object[]> findHierarchicalRelations(String idThesaurus, Collection<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(conceptIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT hr.id_concept1, hr.id_concept2, hr.role,
                       COALESCE(c2.id_ark, ''), COALESCE(c2.id_handle, '')
                FROM hierarchical_relationship hr
                JOIN concept c2
                    ON c2.id_concept = hr.id_concept2
                    AND c2.id_thesaurus = hr.id_thesaurus
                WHERE hr.id_thesaurus = :idThesaurus
                  AND hr.id_concept1 IN (:conceptIds)
                  AND c2.status <> 'CA'
                  AND (
                        hr.role LIKE 'NT%'
                     OR hr.role LIKE 'BT%'
                     OR hr.role = 'RT'
                  )
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("conceptIds", chunk)
                    .getResultList());
        }
        return result;
    }

    public List<Object[]> findMemberships(String idThesaurus, Collection<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(conceptIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT cgc.idconcept, cgc.idgroup,
                       COALESCE(cg.id_ark, ''), COALESCE(cg.id_handle, '')
                FROM concept_group_concept cgc
                LEFT JOIN concept_group cg
                    ON LOWER(cg.idgroup) = LOWER(cgc.idgroup)
                    AND cg.idthesaurus = cgc.idthesaurus
                WHERE cgc.idthesaurus = :idThesaurus
                  AND cgc.idconcept IN (:conceptIds)
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("conceptIds", chunk)
                    .getResultList());
        }
        return result;
    }

    public List<Object[]> findGroupLabels(String idThesaurus, Collection<String> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(groupIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT cgl.idgroup, cgl.lang, cgl.lexicalvalue
                FROM concept_group_label cgl
                WHERE cgl.idthesaurus = :idThesaurus
                  AND cgl.idgroup IN (:groupIds)
                ORDER BY cgl.idgroup, cgl.lang
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("groupIds", chunk)
                    .getResultList());
        }
        return result;
    }

    public List<Object[]> findExactMatches(String idThesaurus, Collection<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(conceptIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT a.internal_id_concept, a.uri_target
                FROM alignement a
                WHERE a.internal_id_thesaurus = :idThesaurus
                  AND a.internal_id_concept IN (:conceptIds)
                  AND a.alignement_id_type = 1
                  AND a.uri_target IS NOT NULL
                  AND a.uri_target <> ''
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("conceptIds", chunk)
                    .getResultList());
        }
        return result;
    }

    public List<Object[]> findReplacedBy(String idThesaurus, Collection<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(conceptIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT cr.id_concept1, cr.id_concept2,
                       COALESCE(c.id_ark, ''), COALESCE(c.id_handle, '')
                FROM concept_replacedby cr
                JOIN concept c
                    ON c.id_concept = cr.id_concept2
                    AND c.id_thesaurus = cr.id_thesaurus
                WHERE cr.id_thesaurus = :idThesaurus
                  AND cr.id_concept1 IN (:conceptIds)
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("conceptIds", chunk)
                    .getResultList());
        }
        return result;
    }

    public List<Object[]> findReplaces(String idThesaurus, Collection<String> conceptIds) {
        if (conceptIds == null || conceptIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> result = new ArrayList<>();
        for (List<String> chunk : chunk(conceptIds)) {
            result.addAll(em.createNativeQuery("""
                SELECT cr.id_concept2, cr.id_concept1,
                       COALESCE(c.id_ark, ''), COALESCE(c.id_handle, '')
                FROM concept_replacedby cr
                JOIN concept c
                    ON c.id_concept = cr.id_concept1
                    AND c.id_thesaurus = cr.id_thesaurus
                WHERE cr.id_thesaurus = :idThesaurus
                  AND cr.id_concept2 IN (:conceptIds)
                """)
                    .setParameter("idThesaurus", idThesaurus)
                    .setParameter("conceptIds", chunk)
                    .getResultList());
        }
        return result;
    }

    private static List<List<String>> chunk(Collection<String> values) {
        List<String> list = values instanceof List<String> typed
                ? typed
                : new ArrayList<>(values);
        if (list.size() <= IN_CLAUSE_CHUNK) {
            return List.of(list);
        }
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < list.size(); i += IN_CLAUSE_CHUNK) {
            chunks.add(list.subList(i, Math.min(i + IN_CLAUSE_CHUNK, list.size())));
        }
        return chunks;
    }
}
