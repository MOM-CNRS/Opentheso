package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.shared.repository.projection.ThesaurusLanguageRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ThesaurusSettingsQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<String> findThesaurusTitle(String thesaurusId, String workLang) {
        String lang = StringUtils.isBlank(workLang) ? "fr" : workLang;
        String sql = """
                SELECT COALESCE(
                    (SELECT tl.title
                     FROM thesaurus_label tl
                     LEFT JOIN preferences p ON p.id_thesaurus = tl.id_thesaurus
                     WHERE tl.id_thesaurus = :thesaurusId
                       AND tl.lang = COALESCE(p.source_lang, :lang)
                     LIMIT 1),
                    :thesaurusId
                )
                """;
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public Optional<Integer> findEffectiveRoleOnThesaurus(int userId, String thesaurusId) {
        String sql = """
                SELECT COALESCE(
                    (SELECT uro.id_role
                     FROM user_role_only_on uro
                     WHERE uro.id_user = :userId
                       AND uro.id_theso = :thesaurusId
                     LIMIT 1),
                    (SELECT urg.id_role
                     FROM user_role_group urg
                     JOIN user_group_thesaurus ugt ON urg.id_group = ugt.id_group
                     WHERE urg.id_user = :userId
                       AND ugt.id_thesaurus = :thesaurusId
                     LIMIT 1)
                ) AS id_role
                """;
        @SuppressWarnings("unchecked")
        List<Number> rows = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0).intValue());
    }

    @SuppressWarnings("unchecked")
    public List<ThesaurusLanguageRow> findUsedLanguages(String thesaurusId, String workLang) {
        String lang = StringUtils.isBlank(workLang) ? "fr" : workLang;
        String sql = """
                SELECT ROW_NUMBER() OVER () AS id,
                       l.iso639_1 AS code,
                       l.code_pays AS code_flag,
                       tl.title AS label_theso,
                       CASE WHEN :lang = 'fr' THEN l.french_name ELSE l.english_name END AS display_label
                FROM thesaurus_label tl
                JOIN languages_iso639 l ON tl.lang = l.iso639_1
                WHERE tl.id_thesaurus = :thesaurusId
                ORDER BY l.french_name
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream()
                .map(row -> new ThesaurusLanguageRow(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        row[2] != null ? (String) row[2] : "",
                        row[3] != null ? (String) row[3] : "",
                        row[4] != null ? (String) row[4] : ""
                ))
                .toList();
    }

    public Optional<String> findSourceLanguage(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return Optional.empty();
        }
        String sql = """
                SELECT source_lang
                FROM preferences
                WHERE id_thesaurus = :thesaurusId
                LIMIT 1
                """;
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId.trim())
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }
}
