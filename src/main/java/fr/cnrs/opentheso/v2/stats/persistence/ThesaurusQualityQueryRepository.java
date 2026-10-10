package fr.cnrs.opentheso.v2.stats.persistence;

import fr.cnrs.opentheso.v2.stats.model.ConceptMissingDefinition;
import fr.cnrs.opentheso.v2.stats.model.ConceptToTranslate;
import fr.cnrs.opentheso.v2.stats.model.DefinitionCoverage;
import fr.cnrs.opentheso.v2.stats.model.LanguageCoverageBucket;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ThesaurusQualityQueryRepository {

    private static final String ACTIVE_CONCEPT = "(c.status IS NULL OR c.status NOT IN ('CA', 'DEP'))";

    private final JdbcTemplate jdbcTemplate;

    public ThesaurusQualityQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public double definitionCoverage(String thesaurusId, String sourceLang) {
        String sql = """
                SELECT COALESCE(ROUND(
                    (SELECT COUNT(identifier) FROM note
                     WHERE id_thesaurus = ? AND LOWER(TRIM(lang)) = LOWER(TRIM(?)) AND notetypecode = 'definition')
                    * 100.0 / NULLIF((SELECT COUNT(DISTINCT id_concept) FROM concept WHERE id_thesaurus = ?), 0),
                2), 0)
                """;
        return queryDouble(sql, thesaurusId, sourceLang, thesaurusId);
    }

    public double translatedTermsCoverage(String thesaurusId, String sourceLang) {
        String sql = """
                SELECT COALESCE(ROUND(
                    100.0 * COUNT(*) FILTER (WHERE nb_langues_secondaires > 0) / NULLIF(COUNT(*), 0),
                    2), 0)
                FROM (
                    SELECT pt.id_concept,
                           COUNT(DISTINCT t.lang) FILTER (
                               WHERE LOWER(TRIM(t.lang)) <> LOWER(TRIM(?))
                           ) AS nb_langues_secondaires
                    FROM preferred_term pt
                    JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                    WHERE pt.id_thesaurus = ?
                    GROUP BY pt.id_concept
                    HAVING COUNT(*) FILTER (WHERE LOWER(TRIM(t.lang)) = LOWER(TRIM(?))) > 0
                ) stats
                """;
        return queryDouble(sql, sourceLang, thesaurusId, sourceLang);
    }

    public double translatedDefinitionsCoverage(String thesaurusId, String sourceLang) {
        String sql = """
                SELECT CASE WHEN COUNT(DISTINCT c.id_concept) = 0 THEN 0
                            ELSE COUNT(DISTINCT CASE WHEN n.identifier IS NOT NULL THEN c.id_concept END)
                                 * 100.0 / COUNT(DISTINCT c.id_concept)
                       END
                FROM concept c
                LEFT JOIN note n
                       ON n.identifier = c.id_concept
                      AND n.id_thesaurus = c.id_thesaurus
                      AND n.notetypecode = 'definition'
                      AND NULLIF(TRIM(n.lang), '') IS NOT NULL
                      AND LOWER(TRIM(n.lang)) <> LOWER(TRIM(?))
                WHERE c.id_thesaurus = ?
                """;
        return queryDouble(sql, sourceLang, thesaurusId);
    }

    public double alignmentCoverage(String thesaurusId) {
        String sql = """
                SELECT CASE WHEN COUNT(DISTINCT c.id_concept) = 0 THEN 0
                            ELSE COUNT(DISTINCT CASE WHEN a.internal_id_concept IS NOT NULL THEN c.id_concept END)
                                 * 100.0 / COUNT(DISTINCT c.id_concept)
                       END
                FROM concept c
                LEFT JOIN alignement a
                       ON a.internal_id_concept = c.id_concept
                      AND a.internal_id_thesaurus = c.id_thesaurus
                WHERE c.id_thesaurus = ?
                """;
        return queryDouble(sql, thesaurusId);
    }

    public double arkCoverage(String thesaurusId) {
        String sql = """
                SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE COUNT(*) FILTER (WHERE NULLIF(TRIM(id_ark), '') IS NOT NULL) * 100.0 / COUNT(*)
                       END
                FROM concept
                WHERE id_thesaurus = ?
                """;
        return queryDouble(sql, thesaurusId);
    }

    public double rtCoverage(String thesaurusId) {
        String sql = """
                SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE COUNT(*) FILTER (
                                WHERE EXISTS (
                                    SELECT 1 FROM hierarchical_relationship hr
                                    WHERE hr.id_thesaurus = c.id_thesaurus
                                      AND hr.role = 'RT'
                                      AND (hr.id_concept1 = c.id_concept OR hr.id_concept2 = c.id_concept)
                                )
                            ) * 100.0 / COUNT(*)
                       END
                FROM concept c
                WHERE c.id_thesaurus = ?
                """;
        return queryDouble(sql, thesaurusId);
    }

    /** Percentage of active concepts that have at least one BT or NT. */
    public double hierarchyCoverage(String thesaurusId) {
        String sql = """
                SELECT CASE WHEN COUNT(*) = 0 THEN 0
                            ELSE COUNT(*) FILTER (
                                WHERE EXISTS (
                                    SELECT 1 FROM hierarchical_relationship hr
                                    WHERE hr.id_thesaurus = c.id_thesaurus
                                      AND hr.role IN ('BT', 'NT')
                                      AND hr.id_concept1 = c.id_concept
                                )
                            ) * 100.0 / COUNT(*)
                       END
                FROM concept c
                WHERE c.id_thesaurus = ?
                  AND %s
                """.formatted(ACTIVE_CONCEPT);
        return queryDouble(sql, thesaurusId);
    }

    public LocalDateTime lastModification(String thesaurusId) {
        try {
            Timestamp timestamp = jdbcTemplate.queryForObject(
                    "SELECT MAX(modified) FROM concept WHERE id_thesaurus = ?",
                    Timestamp.class,
                    thesaurusId
            );
            return timestamp == null ? null : timestamp.toLocalDateTime();
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<String> thesaurusLanguages(String thesaurusId) {
        return jdbcTemplate.queryForList(
                """
                        SELECT DISTINCT UPPER(TRIM(t.lang))
                        FROM term t
                        WHERE t.id_thesaurus = ?
                          AND NULLIF(TRIM(t.lang), '') IS NOT NULL
                        ORDER BY 1
                        """,
                String.class,
                thesaurusId
        );
    }

    public List<LanguageCoverageBucket> languageCoverage(String thesaurusId) {
        String sql = """
                SELECT nb_langues, COUNT(*) AS nb_concepts
                FROM (
                    SELECT c.id_concept, COUNT(DISTINCT t.lang) AS nb_langues
                    FROM concept c
                    LEFT JOIN preferred_term pt
                        ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                    LEFT JOIN term t
                        ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                    WHERE c.id_thesaurus = ?
                      AND %s
                    GROUP BY c.id_concept
                ) per_concept
                GROUP BY nb_langues
                ORDER BY nb_langues
                """.formatted(ACTIVE_CONCEPT);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new LanguageCoverageBucket(
                rs.getInt("nb_langues"),
                rs.getLong("nb_concepts")
        ), thesaurusId);
    }

    public long countConceptsByLanguageCount(String thesaurusId, int languageCount) {
        String sql = """
                SELECT COUNT(*) FROM (
                    SELECT c.id_concept
                    FROM concept c
                    LEFT JOIN preferred_term pt
                        ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                    LEFT JOIN term t
                        ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                    WHERE c.id_thesaurus = ?
                      AND %s
                    GROUP BY c.id_concept
                    HAVING COUNT(DISTINCT t.lang) = ?
                ) counted
                """.formatted(ACTIVE_CONCEPT);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, thesaurusId, languageCount);
        return count == null ? 0L : count;
    }

    public List<ConceptToTranslate> conceptsByLanguageCount(
            String thesaurusId, int languageCount, String sourceLang, int limit
    ) {
        String sql = """
                SELECT c.id_concept,
                       MAX(CASE WHEN LOWER(t.lang) = LOWER(?) THEN t.lexical_value END) AS label,
                       STRING_AGG(DISTINCT UPPER(t.lang), ', ' ORDER BY UPPER(t.lang)) AS existing_langs
                FROM concept c
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                WHERE c.id_thesaurus = ?
                  AND %s
                GROUP BY c.id_concept
                HAVING COUNT(DISTINCT t.lang) = ?
                ORDER BY label NULLS FIRST
                LIMIT ?
                """.formatted(ACTIVE_CONCEPT);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ConceptToTranslate(
                rs.getString("id_concept"),
                rs.getString("label"),
                rs.getString("existing_langs")
        ), sourceLang, thesaurusId, languageCount, Math.max(0, limit));
    }

    public List<DefinitionCoverage> definitionCoverageByLanguage(String thesaurusId) {
        String sql = """
                WITH langs AS (
                    SELECT DISTINCT lang FROM term WHERE id_thesaurus = ?
                ),
                active_concepts AS (
                    SELECT id_concept FROM concept
                    WHERE id_thesaurus = ? AND (status IS NULL OR status NOT IN ('CA', 'DEP'))
                ),
                total AS (
                    SELECT COUNT(*) AS total_concepts FROM active_concepts
                )
                SELECT l.lang,
                       COUNT(DISTINCT n.identifier) AS with_def,
                       t.total_concepts,
                       COALESCE(ROUND(100.0 * COUNT(DISTINCT n.identifier) / NULLIF(t.total_concepts, 0), 2), 0)
                           AS coverage_percent
                FROM langs l
                CROSS JOIN total t
                LEFT JOIN note n
                    ON n.id_thesaurus = ?
                   AND n.notetypecode = 'definition'
                   AND LOWER(n.lang) = LOWER(l.lang)
                   AND n.identifier IN (SELECT id_concept FROM active_concepts)
                GROUP BY l.lang, t.total_concepts
                ORDER BY coverage_percent DESC NULLS LAST
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DefinitionCoverage(
                rs.getString("lang"),
                rs.getLong("with_def"),
                rs.getLong("total_concepts"),
                rs.getDouble("coverage_percent")
        ), thesaurusId, thesaurusId, thesaurusId);
    }

    public long countMissingDefinitions(String thesaurusId, String lang) {
        String sql = """
                SELECT COUNT(*) FROM concept c
                WHERE c.id_thesaurus = ?
                  AND %s
                  AND NOT EXISTS (
                      SELECT 1 FROM note n
                      WHERE n.identifier = c.id_concept
                        AND n.id_thesaurus = c.id_thesaurus
                        AND n.notetypecode = 'definition'
                        AND LOWER(n.lang) = LOWER(?)
                  )
                """.formatted(ACTIVE_CONCEPT);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, thesaurusId, lang);
        return count == null ? 0L : count;
    }

    public List<ConceptMissingDefinition> missingDefinitions(
            String thesaurusId, String lang, String sourceLang, int limit
    ) {
        String sql = """
                SELECT c.id_concept,
                       MAX(CASE WHEN LOWER(t.lang) = LOWER(?) THEN t.lexical_value END) AS label
                FROM concept c
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term t
                    ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                WHERE c.id_thesaurus = ?
                  AND %s
                  AND NOT EXISTS (
                      SELECT 1 FROM note n
                      WHERE n.identifier = c.id_concept
                        AND n.id_thesaurus = c.id_thesaurus
                        AND n.notetypecode = 'definition'
                        AND LOWER(n.lang) = LOWER(?)
                  )
                GROUP BY c.id_concept
                ORDER BY label NULLS FIRST
                LIMIT ?
                """.formatted(ACTIVE_CONCEPT);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ConceptMissingDefinition(
                rs.getString("id_concept"),
                rs.getString("label")
        ), sourceLang, thesaurusId, lang, Math.max(0, limit));
    }

    private double queryDouble(String sql, Object... args) {
        try {
            Double value = jdbcTemplate.queryForObject(sql, Double.class, args);
            return value == null || value.isNaN() ? 0d : value;
        } catch (EmptyResultDataAccessException e) {
            return 0d;
        }
    }
}
