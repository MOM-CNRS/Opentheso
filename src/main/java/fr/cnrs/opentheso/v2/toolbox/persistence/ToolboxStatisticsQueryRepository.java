package fr.cnrs.opentheso.v2.toolbox.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Set-based statistics queries (avoids N+1 per collection).
 */
@Repository
@Transactional(readOnly = true)
public class ToolboxStatisticsQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Integer> countConceptsByGroup(String thesaurusId) {
        return toIntMap(entityManager.createNativeQuery("""
                SELECT LOWER(cgc.idgroup), COUNT(c.id_concept)
                FROM concept c
                JOIN concept_group_concept cgc
                    ON c.id_concept = cgc.idconcept
                    AND c.id_thesaurus = cgc.idthesaurus
                WHERE c.id_thesaurus = :thesaurusId
                  AND c.status != 'CA'
                GROUP BY LOWER(cgc.idgroup)
                """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList());
    }

    public Map<String, Integer> countNotesByGroup(String thesaurusId, String language) {
        return toIntMap(entityManager.createNativeQuery("""
                SELECT LOWER(cgc.idgroup), COUNT(n.id)
                FROM note n
                JOIN concept_group_concept cgc
                    ON cgc.idconcept = n.identifier
                    AND cgc.idthesaurus = n.id_thesaurus
                WHERE n.id_thesaurus = :thesaurusId
                  AND n.lang = :language
                GROUP BY LOWER(cgc.idgroup)
                """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("language", language)
                .getResultList());
    }

    public Map<String, Integer> countSynonymsByGroup(String thesaurusId, String language) {
        return toIntMap(entityManager.createNativeQuery("""
                SELECT LOWER(cgc.idgroup), COUNT(npt.id_term)
                FROM non_preferred_term npt
                INNER JOIN preferred_term pt
                    ON pt.id_term = npt.id_term
                    AND pt.id_thesaurus = npt.id_thesaurus
                INNER JOIN concept_group_concept cgc
                    ON cgc.idthesaurus = pt.id_thesaurus
                    AND cgc.idconcept = pt.id_concept
                WHERE npt.lang = :language
                  AND npt.id_thesaurus = :thesaurusId
                GROUP BY LOWER(cgc.idgroup)
                """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("language", language)
                .getResultList());
    }

    public Map<String, int[]> countAlignmentsByGroup(String thesaurusId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT LOWER(cgc.idgroup),
                       COUNT(a.id),
                       COUNT(a.id) FILTER (WHERE a.uri_target ILIKE '%wikidata.org%')
                FROM alignement a
                JOIN concept_group_concept cgc
                    ON cgc.idconcept = a.internal_id_concept
                    AND cgc.idthesaurus = a.internal_id_thesaurus
                WHERE cgc.idthesaurus = :thesaurusId
                GROUP BY LOWER(cgc.idgroup)
                """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        Map<String, int[]> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] == null) {
                continue;
            }
            result.put(row[0].toString(), new int[]{toInt(row[1]), toInt(row[2])});
        }
        return result;
    }

    public int[] countAlignmentsWithoutGroup(String thesaurusId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT COUNT(a.id),
                       COUNT(a.id) FILTER (WHERE a.uri_target ILIKE '%wikidata.org%')
                FROM alignement a
                WHERE a.internal_id_thesaurus = :thesaurusId
                  AND a.internal_id_concept NOT IN (
                      SELECT idconcept FROM concept_group_concept WHERE idthesaurus = :thesaurusId
                  )
                """)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        if (rows.isEmpty()) {
            return new int[]{0, 0};
        }
        Object[] row = rows.get(0);
        return new int[]{toInt(row[0]), toInt(row[1])};
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> toIntMap(List<?> rows) {
        Map<String, Integer> result = new HashMap<>();
        for (Object raw : rows) {
            Object[] row = (Object[]) raw;
            if (row[0] == null) {
                continue;
            }
            result.put(row[0].toString(), toInt(row[1]));
        }
        return result;
    }

    private static int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
