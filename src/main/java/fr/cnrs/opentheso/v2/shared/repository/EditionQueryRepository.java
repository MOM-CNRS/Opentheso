package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.EditionCollectionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusDetailsRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.EditionThesaurusRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.LanguageOptionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.ProjectSummaryRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class EditionQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<EditionThesaurusRow> findAdminThesauriForUser(int userId, boolean superAdmin, String workLang) {
        String lang = StringUtils.isBlank(workLang) ? "fr" : workLang;
        String sql = """
                SELECT
                    t.id_thesaurus,
                    COALESCE(tl_sub.title, NULLIF(pref.preferredname, t.id_thesaurus), t.id_thesaurus) AS thesaurus_title,
                    COALESCE(t."private", false) AS is_private,
                    t.created
                FROM thesaurus t
                LEFT JOIN preferences pref ON pref.id_thesaurus = t.id_thesaurus
                LEFT JOIN LATERAL (
                    SELECT tl.title
                    FROM thesaurus_label tl
                    WHERE tl.id_thesaurus = t.id_thesaurus
                      AND tl.title IS NOT NULL
                      AND BTRIM(tl.title) <> ''
                    ORDER BY
                      CASE
                        WHEN tl.lang = COALESCE(pref.source_lang, :lang) THEN 0
                        WHEN tl.lang = :lang THEN 1
                        ELSE 2
                      END
                    LIMIT 1
                ) tl_sub ON true
                WHERE (
                    :superAdmin = true
                    OR EXISTS (
                        SELECT 1
                        FROM user_role_only_on uro
                        WHERE uro.id_user = :userId
                          AND uro.id_theso = t.id_thesaurus
                          AND uro.id_role < 3
                    )
                    OR EXISTS (
                        SELECT 1
                        FROM user_role_group urg
                        JOIN user_group_thesaurus ugt ON urg.id_group = ugt.id_group
                        WHERE urg.id_user = :userId
                          AND ugt.id_thesaurus = t.id_thesaurus
                          AND urg.id_role < 3
                    )
                )
                ORDER BY t.created DESC NULLS LAST, LOWER(t.id_thesaurus)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("superAdmin", superAdmin)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream().map(this::toEditionThesaurusRow).toList();
    }

    public int[] countAllConceptStats(String thesaurusId) {
        String sql = """
                SELECT
                    SUM(CASE WHEN c.status NOT IN ('CA', 'DEP', 'dep') THEN 1 ELSE 0 END),
                    SUM(CASE WHEN c.status = 'CA' THEN 1 ELSE 0 END),
                    SUM(CASE WHEN c.status IN ('DEP', 'dep') THEN 1 ELSE 0 END)
                FROM concept c
                WHERE c.id_thesaurus = :thesaurusId
                """;
        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult();
        return new int[]{
                row[0] != null ? ((Number) row[0]).intValue() : 0,
                row[1] != null ? ((Number) row[1]).intValue() : 0,
                row[2] != null ? ((Number) row[2]).intValue() : 0
        };
    }

    @SuppressWarnings("unchecked")
    public List<LanguageOptionRow> findAllLanguages() {
        String sql = """
                SELECT iso639_1, COALESCE(code_pays, ''), french_name, english_name
                FROM languages_iso639
                ORDER BY iso639_1
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql).getResultList();
        return rows.stream()
                .map(row -> new LanguageOptionRow(
                        (String) row[0],
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? (String) row[2] : "",
                        row[3] != null ? (String) row[3] : ""
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ProjectSummaryRow> findAdminProjectsForUser(int userId) {
        String sql = """
                SELECT ugl.id_group, ugl.label_group
                FROM user_group_label ugl
                JOIN user_role_group urg ON urg.id_group = ugl.id_group
                WHERE urg.id_user = :userId
                  AND urg.id_role < 3
                ORDER BY LOWER(ugl.label_group)
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getResultList();
        return rows.stream()
                .map(row -> new ProjectSummaryRow(((Number) row[0]).intValue(), (String) row[1]))
                .toList();
    }

    public Optional<EditionThesaurusDetailsRow> findThesaurusDetails(String thesaurusId, String workLang) {
        String lang = StringUtils.isBlank(workLang) ? "fr" : workLang;
        String sql = """
                SELECT
                    t.id_thesaurus,
                    COALESCE(tl_sub.title, NULLIF(p.preferredname, t.id_thesaurus), t.id_thesaurus) AS title,
                    COALESCE(t.id_ark, '') AS ark_id,
                    COALESCE(t."private", false) AS is_private,
                    COALESCE(p.source_lang, :lang) AS source_lang
                FROM thesaurus t
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
                WHERE t.id_thesaurus = :thesaurusId
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        return Optional.of(new EditionThesaurusDetailsRow(
                (String) row[0],
                row[1] != null ? (String) row[1] : (String) row[0],
                row[2] != null ? (String) row[2] : "",
                row[3] != null && (Boolean) row[3],
                row[4] != null ? (String) row[4] : lang
        ));
    }

    @SuppressWarnings("unchecked")
    public List<EditionCollectionRow> findCollections(String thesaurusId, String workLang) {
        String lang = StringUtils.isBlank(workLang) ? "fr" : workLang;
        String sql = """
                SELECT
                    cg.idgroup,
                    COALESCE(cgl.lexicalvalue, cg.idgroup) AS label,
                    COALESCE(cg."private", false) AS is_private,
                    parent_rg.id_group1 AS parent_id
                FROM concept_group cg
                LEFT JOIN concept_group_label cgl
                    ON LOWER(cgl.idgroup) = LOWER(cg.idgroup)
                    AND cgl.idthesaurus = cg.idthesaurus
                    AND cgl.lang = :lang
                LEFT JOIN relation_group parent_rg
                    ON LOWER(parent_rg.id_group2) = LOWER(cg.idgroup)
                    AND parent_rg.id_thesaurus = cg.idthesaurus
                    AND parent_rg.relation = 'sub'
                WHERE cg.idthesaurus = :thesaurusId
                ORDER BY LOWER(COALESCE(cgl.lexicalvalue, cg.idgroup))
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream()
                .map(row -> new EditionCollectionRow(
                        (String) row[0],
                        row[1] != null ? (String) row[1] : (String) row[0],
                        row[2] != null && (Boolean) row[2],
                        row[3] != null ? (String) row[3] : null
                ))
                .toList();
    }

    @Transactional
    public void updateCollectionsVisibility(String thesaurusId, List<String> groupIds, boolean isPrivate) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        entityManager.createNativeQuery("""
                UPDATE concept_group
                SET "private" = :isPrivate
                WHERE idthesaurus = :thesaurusId
                  AND idgroup IN (:groupIds)
                """)
                .setParameter("isPrivate", isPrivate)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("groupIds", groupIds)
                .executeUpdate();
    }

    private EditionThesaurusRow toEditionThesaurusRow(Object[] row) {
        LocalDateTime createdAt = null;
        if (row[3] instanceof Timestamp timestamp) {
            createdAt = timestamp.toLocalDateTime();
        } else if (row[3] instanceof LocalDateTime localDateTime) {
            createdAt = localDateTime;
        }
        return new EditionThesaurusRow(
                (String) row[0],
                row[1] != null ? (String) row[1] : (String) row[0],
                row[2] != null && (Boolean) row[2],
                createdAt
        );
    }
}
