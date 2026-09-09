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
    // modifié par #MR, c'est la bonne approche pour récupérer les thésaurus publics
    public List<ConsultationThesaurusOption> findPublicThesauri(int projectId, String lang) {
        String sql = """
                SELECT
                    t.id_thesaurus,
                    tl.title,
                    p.source_lang
                 FROM thesaurus t
                 JOIN preferences p ON p.id_thesaurus = t.id_thesaurus
                 JOIN thesaurus_label tl ON tl.id_thesaurus = t.id_thesaurus AND tl.lang = p.source_lang
                 WHERE COALESCE(t."private", false) = false
                   AND (
                        :projectId = -1
                        OR EXISTS (
                            SELECT 1
                            FROM user_group_thesaurus gt
                            WHERE gt.id_thesaurus = t.id_thesaurus
                              AND gt.id_group = :projectId
                        )
                   )
                ORDER BY CAST(regexp_replace(t.id_thesaurus, '\\D', '', 'g') AS INTEGER) DESC
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
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
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.PROJECT_ID, projectId)
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

    /**
     * Liste enrichie pour l'écran de sélection de thésaurus (publics + membres).
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findPickerThesaurusRows(Integer userId, boolean superAdmin, String lang) {
        // Sentinelle : un userId null non typé fait échouer PostgreSQL (42P18).
        int resolvedUserId = userId == null ? -1 : userId;
        String sql = """
                WITH titled AS (
                    SELECT
                        t.id_thesaurus,
                        COALESCE(tl_sub.title, t.id_thesaurus) AS title,
                        COALESCE(t."private", false) AS is_private,
                        t.created,
                        COALESCE(p.source_lang, CAST(:lang AS text)) AS source_lang,
                        COALESCE(tl_sub.subject, '') AS domain,
                        COALESCE(tl_sub.publisher, '') AS organization,
                        COALESCE(tl_sub.coverage, '') AS chronology
                    FROM thesaurus t
                    LEFT JOIN preferences p ON p.id_thesaurus = t.id_thesaurus
                    LEFT JOIN LATERAL (
                        SELECT tl.title, tl.subject, tl.publisher, tl.coverage
                        FROM thesaurus_label tl
                        WHERE tl.id_thesaurus = t.id_thesaurus
                          AND tl.title IS NOT NULL
                          AND BTRIM(tl.title) <> ''
                        ORDER BY
                          CASE
                            WHEN tl.lang = COALESCE(p.source_lang, CAST(:lang AS text)) THEN 0
                            WHEN tl.lang = CAST(:lang AS text) THEN 1
                            ELSE 2
                          END
                        LIMIT 1
                    ) tl_sub ON true
                ),
                member_roles AS (
                    SELECT
                        roles.id_thesaurus,
                        MIN(roles.id_role) AS id_role
                    FROM (
                        SELECT
                            ugt.id_thesaurus,
                            urg.id_role
                        FROM user_role_group urg
                        JOIN user_group_thesaurus ugt ON ugt.id_group = urg.id_group
                        WHERE urg.id_user = CAST(:userId AS integer)

                        UNION ALL

                        SELECT
                            uro.id_theso AS id_thesaurus,
                            uro.id_role
                        FROM user_role_only_on uro
                        WHERE uro.id_user = CAST(:userId AS integer)
                          AND uro.id_theso IS NOT NULL
                          AND BTRIM(uro.id_theso) <> ''

                        UNION ALL

                        -- Créateur (ex. super-admin sans ligne user_role_*)
                        SELECT
                            tl.id_thesaurus,
                            2 AS id_role
                        FROM thesaurus_label tl
                        JOIN users u ON LOWER(BTRIM(u.username)) = LOWER(BTRIM(tl.creator))
                        WHERE u.id_user = CAST(:userId AS integer)
                          AND tl.creator IS NOT NULL
                          AND BTRIM(tl.creator) <> ''
                    ) roles
                    WHERE CAST(:userId AS integer) >= 0
                    GROUP BY roles.id_thesaurus
                )
                SELECT
                    titled.id_thesaurus,
                    titled.title,
                    titled.is_private,
                    titled.created,
                    CASE
                        WHEN member_roles.id_thesaurus IS NOT NULL THEN 'member'
                        ELSE 'public'
                    END AS access_kind,
                    COALESCE((
                        SELECT COUNT(c.id_concept)
                        FROM concept c
                        WHERE c.id_thesaurus = titled.id_thesaurus
                          AND COALESCE(c.status, '') <> 'CA'
                    ), 0) AS term_count,
                    COALESCE((
                        SELECT string_agg(DISTINCT ugl.label_group, ', ' ORDER BY ugl.label_group)
                        FROM user_group_thesaurus ugt
                        JOIN user_group_label ugl ON ugl.id_group = ugt.id_group
                        WHERE ugt.id_thesaurus = titled.id_thesaurus
                    ), '') AS projects,
                    COALESCE((
                        SELECT string_agg(DISTINCT tl.lang, ',' ORDER BY tl.lang)
                        FROM thesaurus_label tl
                        WHERE tl.id_thesaurus = titled.id_thesaurus
                          AND tl.lang IS NOT NULL
                          AND BTRIM(tl.lang) <> ''
                    ), '') AS langs,
                    titled.domain,
                    titled.organization,
                    titled.chronology,
                    member_roles.id_role
                FROM titled
                LEFT JOIN member_roles ON member_roles.id_thesaurus = titled.id_thesaurus
                WHERE
                    (
                        CAST(:userId AS integer) < 0
                        AND titled.is_private = false
                    )
                    OR (
                        CAST(:userId AS integer) >= 0
                        AND (
                            CAST(:superAdmin AS boolean) = true
                            OR member_roles.id_thesaurus IS NOT NULL
                            OR titled.is_private = false
                        )
                    )
                ORDER BY LOWER(titled.title), titled.id_thesaurus
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.USER_ID, resolvedUserId)
                .setParameter("superAdmin", superAdmin)
                .setParameter("lang", lang)
                .getResultList();
    }
}
