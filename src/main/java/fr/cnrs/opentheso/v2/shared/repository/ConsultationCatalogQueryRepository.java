package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.concept.model.ConsultationProjectOption;
import fr.cnrs.opentheso.v2.concept.model.ConsultationThesaurusOption;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ConsultationCatalogQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ConsultationProjectOption> findPublicProjects() {
        String sql = """
                SELECT ugl.id_group, ugl.label_group
                FROM user_group_thesaurus ugt
                JOIN user_group_label ugl ON ugl.id_group = ugt.id_group
                JOIN thesaurus t ON t.id_thesaurus = ugt.id_thesaurus
                WHERE COALESCE(t."private", false) = false
                GROUP BY ugl.id_group, ugl.label_group
                ORDER BY LOWER(ugl.label_group)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        return rows.stream()
                .map(row -> new ConsultationProjectOption(((Number) row[0]).intValue(), (String) row[1]))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ConsultationThesaurusOption> findPublicThesauri(int projectId, String lang) {
        String sql = """
                SELECT
                    t.id_thesaurus,
                    COALESCE(tl_sub.title, NULLIF(p.preferredname, t.id_thesaurus), t.id_thesaurus) AS thesaurus_title,
                    COALESCE(p.source_lang, :lang) AS default_lang
                FROM thesaurus t
                JOIN user_group_thesaurus ugt ON ugt.id_thesaurus = t.id_thesaurus
                LEFT JOIN preferences p ON p.id_thesaurus = t.id_thesaurus
                LEFT JOIN LATERAL (
                    SELECT tl.title
                    FROM thesaurus_label tl
                    WHERE tl.id_thesaurus = t.id_thesaurus
                      AND tl.title IS NOT NULL
                      AND BTRIM(tl.title) <> ''
                    ORDER BY
                      CASE
                        WHEN tl.lang = COALESCE(p.source_lang, :lang) THEN 0
                        WHEN tl.lang = :lang THEN 1
                        ELSE 2
                      END
                    LIMIT 1
                ) tl_sub ON true
                WHERE COALESCE(t."private", false) = false
                  AND (:projectId < 0 OR ugt.id_group = :projectId)
                ORDER BY LOWER(t.id_thesaurus)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("projectId", projectId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream()
                .map(row -> new ConsultationThesaurusOption(
                        (String) row[0],
                        (String) row[1],
                        row[2] != null ? (String) row[2] : lang
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ConsultationThesaurusOption> findAccessibleThesauriForUser(int userId, int projectId, String lang) {
        String sql = """
                SELECT
                    ugt.id_thesaurus,
                    COALESCE(tl_sub.title, NULLIF(p.preferredname, ugt.id_thesaurus), ugt.id_thesaurus) AS thesaurus_title,
                    COALESCE(p.source_lang, :lang) AS default_lang
                FROM user_group_label ugl
                JOIN user_group_thesaurus ugt ON ugt.id_group = ugl.id_group
                JOIN thesaurus t ON t.id_thesaurus = ugt.id_thesaurus
                LEFT JOIN preferences p ON p.id_thesaurus = ugt.id_thesaurus
                LEFT JOIN LATERAL (
                    SELECT tl.title
                    FROM thesaurus_label tl
                    WHERE tl.id_thesaurus = ugt.id_thesaurus
                      AND tl.title IS NOT NULL
                      AND BTRIM(tl.title) <> ''
                    ORDER BY
                      CASE
                        WHEN tl.lang = COALESCE(p.source_lang, :lang) THEN 0
                        WHEN tl.lang = :lang THEN 1
                        ELSE 2
                      END
                    LIMIT 1
                ) tl_sub ON true
                WHERE (:projectId < 0 OR ugl.id_group = :projectId)
                  AND (
                      EXISTS (SELECT 1 FROM user_role_group urg WHERE urg.id_user = :userId AND urg.id_group = ugl.id_group)
                      OR EXISTS (SELECT 1 FROM user_role_only_on uro WHERE uro.id_user = :userId AND uro.id_group = ugl.id_group)
                  )
                ORDER BY LOWER(ugt.id_thesaurus)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("projectId", projectId)
                .setParameter("lang", lang)
                .getResultList();
        Map<String, ConsultationThesaurusOption> unique = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String id = (String) row[0];
            unique.putIfAbsent(id, new ConsultationThesaurusOption(
                    id,
                    (String) row[1],
                    row[2] != null ? (String) row[2] : lang
            ));
        }
        return List.copyOf(unique.values());
    }
}
