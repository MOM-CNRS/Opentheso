package fr.cnrs.opentheso.v2.sync.repository;

import fr.cnrs.opentheso.v2.sync.model.SyncPendingConcept;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class ThesaurusSyncQueryRepository {

    public static final int PREVIEW_LIMIT = 300;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Concepts à envoyer, avec libellé et familles de champs touchés localement.
     * {@code since == null} : premier envoi (tous les concepts, sans détail de champ).
     */
    @SuppressWarnings("unchecked")
    public List<SyncPendingConcept> findPendingConcepts(String thesaurusId, Date since, String lang, int limit) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        int resolvedLimit = limit <= 0 ? PREVIEW_LIMIT : limit;
        String resolvedLang = StringUtils.defaultIfBlank(lang, "fr");
        boolean firstSend = since == null;

        String sql;
        if (firstSend) {
            sql = """
                    SELECT
                        c.id_concept,
                        COALESCE(tl.lexical_value, c.id_concept) AS label,
                        false, false, false, false
                    FROM concept c
                    LEFT JOIN LATERAL (
                        SELECT t.lexical_value
                        FROM preferred_term pt
                        JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                        WHERE pt.id_concept = c.id_concept
                          AND pt.id_thesaurus = c.id_thesaurus
                        ORDER BY
                          CASE WHEN t.lang = CAST(:lang AS text) THEN 0 ELSE 1 END,
                          t.lang
                        LIMIT 1
                    ) tl ON true
                    WHERE c.id_thesaurus = :idThesaurus
                      AND c.status <> 'CA'
                    ORDER BY LOWER(COALESCE(tl.lexical_value, c.id_concept)), c.id_concept
                    """;
        } else {
            sql = """
                    SELECT
                        c.id_concept,
                        COALESCE(tl.lexical_value, c.id_concept) AS label,
                        (c.modified > :startDate) AS concept_changed,
                        EXISTS (
                            SELECT 1
                            FROM preferred_term pt
                            JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                            WHERE pt.id_concept = c.id_concept
                              AND pt.id_thesaurus = c.id_thesaurus
                              AND t.modified > :startDate
                        ) AS pref_changed,
                        EXISTS (
                            SELECT 1
                            FROM preferred_term pt
                            JOIN non_preferred_term npt
                              ON npt.id_term = pt.id_term AND npt.id_thesaurus = pt.id_thesaurus
                            WHERE pt.id_concept = c.id_concept
                              AND pt.id_thesaurus = c.id_thesaurus
                              AND npt.modified > :startDate
                        ) AS alt_changed,
                        EXISTS (
                            SELECT 1
                            FROM note n
                            WHERE n.id_thesaurus = c.id_thesaurus
                              AND n.modified > :startDate
                              AND (
                                    n.id_concept = c.id_concept
                                    OR n.id_term IN (
                                        SELECT pt.id_term
                                        FROM preferred_term pt
                                        WHERE pt.id_concept = c.id_concept
                                          AND pt.id_thesaurus = c.id_thesaurus
                                    )
                              )
                        ) AS note_changed
                    FROM concept c
                    LEFT JOIN LATERAL (
                        SELECT t.lexical_value
                        FROM preferred_term pt
                        JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                        WHERE pt.id_concept = c.id_concept
                          AND pt.id_thesaurus = c.id_thesaurus
                        ORDER BY
                          CASE WHEN t.lang = CAST(:lang AS text) THEN 0 ELSE 1 END,
                          t.lang
                        LIMIT 1
                    ) tl ON true
                    WHERE c.id_thesaurus = :idThesaurus
                      AND c.status <> 'CA'
                      AND (
                            c.modified > :startDate
                            OR EXISTS (
                                SELECT 1
                                FROM preferred_term pt
                                JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                                WHERE pt.id_concept = c.id_concept
                                  AND pt.id_thesaurus = c.id_thesaurus
                                  AND t.modified > :startDate
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM preferred_term pt
                                JOIN non_preferred_term npt
                                  ON npt.id_term = pt.id_term AND npt.id_thesaurus = pt.id_thesaurus
                                WHERE pt.id_concept = c.id_concept
                                  AND pt.id_thesaurus = c.id_thesaurus
                                  AND npt.modified > :startDate
                            )
                            OR EXISTS (
                                SELECT 1
                                FROM note n
                                WHERE n.id_thesaurus = c.id_thesaurus
                                  AND n.modified > :startDate
                                  AND (
                                        n.id_concept = c.id_concept
                                        OR n.id_term IN (
                                            SELECT pt.id_term
                                            FROM preferred_term pt
                                            WHERE pt.id_concept = c.id_concept
                                              AND pt.id_thesaurus = c.id_thesaurus
                                        )
                                  )
                            )
                      )
                    ORDER BY LOWER(COALESCE(tl.lexical_value, c.id_concept)), c.id_concept
                    """;
        }

        var query = entityManager.createNativeQuery(sql)
                .setParameter("idThesaurus", thesaurusId)
                .setParameter("lang", resolvedLang)
                .setMaxResults(resolvedLimit);
        if (!firstSend) {
            query.setParameter("startDate", since);
        }
        List<Object[]> rows = query.getResultList();
        List<SyncPendingConcept> pending = new ArrayList<>();
        for (Object[] row : rows) {
            pending.add(toPending(row, firstSend));
        }
        return List.copyOf(pending);
    }

    private static SyncPendingConcept toPending(Object[] row, boolean firstSend) {
        String id = row[0] != null ? String.valueOf(row[0]) : "";
        String label = row[1] != null ? String.valueOf(row[1]) : id;
        if (firstSend) {
            return new SyncPendingConcept(id, label, List.of("all"));
        }
        List<String> fields = new ArrayList<>();
        if (toBoolean(row[3])) {
            fields.add("prefLabel");
        }
        if (toBoolean(row[4])) {
            fields.add("synonym");
        }
        if (toBoolean(row[5])) {
            fields.add("note");
        }
        if (fields.isEmpty() && toBoolean(row[2])) {
            fields.add("concept");
        }
        return new SyncPendingConcept(id, label, fields);
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }
}
