package fr.cnrs.opentheso.v2.stats.persistence;

import fr.cnrs.opentheso.v2.stats.model.ApiCallStat;
import fr.cnrs.opentheso.v2.stats.model.ConceptLangShare;
import fr.cnrs.opentheso.v2.stats.model.LanguageTraffic;
import fr.cnrs.opentheso.v2.stats.model.SearchTermStat;
import fr.cnrs.opentheso.v2.stats.model.SynonymSearchStat;
import fr.cnrs.opentheso.v2.stats.model.ThesaurusTraffic;
import fr.cnrs.opentheso.v2.stats.model.TopConcept;
import fr.cnrs.opentheso.v2.stats.model.TrafficPoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class StatUsageQueryRepository {

    /**
     * Daily totals without double-counting: aggregated rows for dates already in
     * {@code stat_concept_view_daily}, raw events only for dates not yet aggregated
     * (typically today, or yesterday if the nightly job has not run).
     */
    private static final String TRAFFIC_SQL = """
            SELECT stat_date, SUM(nb_vues) AS total_vues
            FROM (
                SELECT stat_date, nb_vues
                FROM stat_concept_view_daily
                WHERE stat_date BETWEEN ? AND ?
                  AND (? IS NULL OR thesaurus_id = ?)
                UNION ALL
                SELECT CAST(event_time AS date) AS stat_date, COUNT(*) AS nb_vues
                FROM stat_log_event e
                WHERE e.event_type = 'CONCEPT_VIEW'
                  AND e.event_time >= ? AND e.event_time < ?
                  AND e.concept_id IS NOT NULL
                  AND (? IS NULL OR e.thesaurus_id = ?)
                  AND NOT EXISTS (
                      SELECT 1 FROM stat_concept_view_daily d
                      WHERE d.stat_date = CAST(e.event_time AS date)
                  )
                GROUP BY CAST(event_time AS date)
            ) combined
            GROUP BY stat_date
            ORDER BY stat_date
            """;

    private static final String BY_THESAURUS_SQL = """
            SELECT thesaurus_id, MAX(thesaurus_label) AS thesaurus_label, SUM(nb_vues) AS total_vues
            FROM (
                SELECT thesaurus_id, thesaurus_label, nb_vues
                FROM stat_concept_view_daily
                WHERE stat_date BETWEEN ? AND ?
                UNION ALL
                SELECT e.thesaurus_id, MAX(e.thesaurus_label), COUNT(*)
                FROM stat_log_event e
                WHERE e.event_type = 'CONCEPT_VIEW'
                  AND e.event_time >= ? AND e.event_time < ?
                  AND e.concept_id IS NOT NULL
                  AND NULLIF(TRIM(e.thesaurus_id), '') IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1 FROM stat_concept_view_daily d
                      WHERE d.stat_date = CAST(e.event_time AS date)
                  )
                GROUP BY e.thesaurus_id
            ) combined
            GROUP BY thesaurus_id
            ORDER BY total_vues DESC
            """;

    /**
     * Language split can only come from raw events: the daily table has no {@code lang}.
     * Raw rows are kept for a year, so this is not limited to the unaggregated day.
     */
    private static final String BY_LANGUAGE_SQL = """
            SELECT LOWER(TRIM(lang)) AS lang, COUNT(*) AS total_vues
            FROM stat_log_event
            WHERE event_type = 'CONCEPT_VIEW'
              AND event_time >= ? AND event_time < ?
              AND concept_id IS NOT NULL
              AND (? IS NULL OR thesaurus_id = ?)
              AND NULLIF(TRIM(lang), '') IS NOT NULL
            GROUP BY LOWER(TRIM(lang))
            ORDER BY total_vues DESC
            """;

    private static final String TOP_CONCEPTS_SQL = """
            WITH concept_totals AS (
                SELECT concept_id, thesaurus_id, SUM(nb_vues) AS total_vues
                FROM (
                    SELECT concept_id, thesaurus_id, nb_vues
                    FROM stat_concept_view_daily
                    WHERE stat_date BETWEEN ? AND ?
                      AND (? IS NULL OR thesaurus_id = ?)
                    UNION ALL
                    SELECT e.concept_id, e.thesaurus_id, COUNT(*)
                    FROM stat_log_event e
                    WHERE e.event_type = 'CONCEPT_VIEW'
                      AND e.event_time >= ? AND e.event_time < ?
                      AND e.concept_id IS NOT NULL
                      AND (? IS NULL OR e.thesaurus_id = ?)
                      AND NOT EXISTS (
                          SELECT 1 FROM stat_concept_view_daily d
                          WHERE d.stat_date = CAST(e.event_time AS date)
                      )
                    GROUP BY e.concept_id, e.thesaurus_id
                ) combined
                GROUP BY concept_id, thesaurus_id
            ),
            labels AS (
                SELECT DISTINCT ON (concept_id, thesaurus_id)
                    concept_id, thesaurus_id, concept_label, thesaurus_label
                FROM stat_log_event
                WHERE event_type = 'CONCEPT_VIEW'
                  AND concept_label IS NOT NULL
                ORDER BY concept_id, thesaurus_id, event_time DESC
            )
            SELECT t.concept_id, t.thesaurus_id,
                   COALESCE(l.concept_label, t.concept_id) AS concept_label,
                   COALESCE(l.thesaurus_label, t.thesaurus_id) AS thesaurus_label,
                   t.total_vues
            FROM concept_totals t
            LEFT JOIN labels l
              ON l.concept_id = t.concept_id AND l.thesaurus_id = t.thesaurus_id
            ORDER BY t.total_vues DESC
            LIMIT ?
            """;

    private static final String KPI_SQL = """
            SELECT
                (
                    SELECT COALESCE(SUM(nb_vues), 0) FROM (
                        SELECT nb_vues FROM stat_concept_view_daily
                        WHERE stat_date BETWEEN ? AND ?
                          AND (? IS NULL OR thesaurus_id = ?)
                        UNION ALL
                        SELECT COUNT(*) FROM stat_log_event e
                        WHERE e.event_type = 'CONCEPT_VIEW'
                          AND e.event_time >= ? AND e.event_time < ?
                          AND e.concept_id IS NOT NULL
                          AND (? IS NULL OR e.thesaurus_id = ?)
                          AND NOT EXISTS (
                              SELECT 1 FROM stat_concept_view_daily d
                              WHERE d.stat_date = CAST(e.event_time AS date)
                          )
                    ) v
                ) AS views,
                (
                    SELECT COUNT(*) FROM stat_log_event
                    WHERE event_type IN ('SEARCH_NO_RESULT', 'SEARCH_RESULT_SELECTED', 'SEARCH_APPLIED')
                      AND event_time >= ? AND event_time < ?
                      AND (? IS NULL OR thesaurus_id = ?)
                ) AS searches,
                (
                    SELECT COUNT(*) FROM stat_log_event
                    WHERE event_type = 'API_CALL'
                      AND event_time >= ? AND event_time < ?
                ) AS api_calls,
                (
                    SELECT COUNT(DISTINCT thesaurus_id) FROM (
                        SELECT thesaurus_id FROM stat_concept_view_daily
                        WHERE stat_date BETWEEN ? AND ?
                          AND NULLIF(TRIM(thesaurus_id), '') IS NOT NULL
                          AND (? IS NULL OR thesaurus_id = ?)
                        UNION
                        SELECT thesaurus_id FROM stat_log_event
                        WHERE event_type = 'CONCEPT_VIEW'
                          AND event_time >= ? AND event_time < ?
                          AND NULLIF(TRIM(thesaurus_id), '') IS NOT NULL
                          AND (? IS NULL OR thesaurus_id = ?)
                    ) t
                ) AS active_thesauri
            """;

    private final JdbcTemplate jdbcTemplate;

    public StatUsageQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record UsageKpis(long views, long searches, long apiCalls, long activeThesauri) {
    }

    public UsageKpis loadKpis(LocalDate from, LocalDate to, String thesaurusId) {
        String th = StatJdbc.blankToNull(thesaurusId);
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay();
        return jdbcTemplate.queryForObject(
                KPI_SQL,
                (rs, rowNum) -> new UsageKpis(
                        rs.getLong("views"),
                        rs.getLong("searches"),
                        rs.getLong("api_calls"),
                        rs.getLong("active_thesauri")
                ),
                StatJdbc.sqlDate(from), StatJdbc.sqlDate(to), th, th,
                StatJdbc.ts(fromTs), StatJdbc.ts(toTs), th, th,
                StatJdbc.ts(fromTs), StatJdbc.ts(toTs), th, th,
                StatJdbc.ts(fromTs), StatJdbc.ts(toTs),
                StatJdbc.sqlDate(from), StatJdbc.sqlDate(to), th, th,
                StatJdbc.ts(fromTs), StatJdbc.ts(toTs), th, th
        );
    }

    public List<TrafficPoint> loadTraffic(LocalDate from, LocalDate to, String thesaurusId) {
        String th = StatJdbc.blankToNull(thesaurusId);
        return jdbcTemplate.query(
                TRAFFIC_SQL,
                (rs, rowNum) -> new TrafficPoint(
                        rs.getDate("stat_date").toLocalDate(),
                        rs.getLong("total_vues")
                ),
                StatJdbc.sqlDate(from), StatJdbc.sqlDate(to), th, th,
                StatJdbc.ts(from.atStartOfDay()), StatJdbc.ts(to.plusDays(1).atStartOfDay()), th, th
        );
    }

    public List<ThesaurusTraffic> loadByThesaurus(LocalDate from, LocalDate to) {
        return jdbcTemplate.query(
                BY_THESAURUS_SQL,
                (rs, rowNum) -> new ThesaurusTraffic(
                        rs.getString("thesaurus_id"),
                        rs.getString("thesaurus_label"),
                        rs.getLong("total_vues")
                ),
                StatJdbc.sqlDate(from), StatJdbc.sqlDate(to),
                StatJdbc.ts(from.atStartOfDay()), StatJdbc.ts(to.plusDays(1).atStartOfDay())
        );
    }

    public List<LanguageTraffic> loadByLanguage(LocalDateTime from, LocalDateTime to, String thesaurusId) {
        String th = StatJdbc.blankToNull(thesaurusId);
        return jdbcTemplate.query(
                BY_LANGUAGE_SQL,
                (rs, rowNum) -> new LanguageTraffic(
                        rs.getString("lang"),
                        rs.getLong("total_vues")
                ),
                StatJdbc.ts(from), StatJdbc.ts(to), th, th
        );
    }

    public List<TopConcept> loadTopConcepts(LocalDate from, LocalDate to, String thesaurusId, int limit) {
        String th = StatJdbc.blankToNull(thesaurusId);
        List<TopConcept> tops = jdbcTemplate.query(
                TOP_CONCEPTS_SQL,
                (rs, rowNum) -> new TopConcept(
                        rs.getString("concept_id"),
                        rs.getString("concept_label"),
                        rs.getString("thesaurus_id"),
                        rs.getString("thesaurus_label"),
                        rs.getLong("total_vues"),
                        List.of()
                ),
                StatJdbc.sqlDate(from), StatJdbc.sqlDate(to), th, th,
                StatJdbc.ts(from.atStartOfDay()), StatJdbc.ts(to.plusDays(1).atStartOfDay()), th, th,
                limit
        );
        attachLanguages(tops, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return tops;
    }

    public List<ApiCallStat> loadApiUsage(LocalDateTime from, LocalDateTime to, int limit) {
        String sql = """
                SELECT http_method, url, COUNT(*) AS nb_appels
                FROM stat_log_event
                WHERE event_type = 'API_CALL'
                  AND event_time >= ? AND event_time < ?
                GROUP BY http_method, url
                ORDER BY nb_appels DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ApiCallStat(
                        rs.getString("http_method"),
                        rs.getString("url"),
                        rs.getLong("nb_appels")
                ),
                StatJdbc.ts(from), StatJdbc.ts(to), limit
        );
    }

    public List<SearchTermStat> loadFailedSearches(LocalDateTime from, LocalDateTime to, String thesaurusId, int limit) {
        return loadTermStats("SEARCH_NO_RESULT", from, to, thesaurusId, limit);
    }

    public List<SearchTermStat> loadGlobalSearches(LocalDateTime from, LocalDateTime to, String thesaurusId, int limit) {
        return loadTermStats("SEARCH_APPLIED", from, to, thesaurusId, limit);
    }

    public List<SynonymSearchStat> loadSynonymSearches(LocalDateTime from, LocalDateTime to, String thesaurusId, int limit) {
        String th = StatJdbc.blankToNull(thesaurusId);
        String sql = """
                SELECT LOWER(TRIM(searched_term)) AS searched_term,
                       selected_term,
                       thesaurus_id,
                       MAX(thesaurus_label) AS thesaurus_label,
                       COUNT(*) AS nb
                FROM stat_log_event
                WHERE event_type = 'SEARCH_RESULT_SELECTED'
                  AND event_time >= ? AND event_time < ?
                  AND (? IS NULL OR thesaurus_id = ?)
                  AND LOWER(TRIM(searched_term)) <> LOWER(TRIM(selected_term))
                GROUP BY LOWER(TRIM(searched_term)), selected_term, thesaurus_id
                ORDER BY nb DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SynonymSearchStat(
                        rs.getString("searched_term"),
                        rs.getString("selected_term"),
                        rs.getString("thesaurus_id"),
                        rs.getString("thesaurus_label"),
                        rs.getLong("nb")
                ),
                StatJdbc.ts(from), StatJdbc.ts(to), th, th, limit
        );
    }

    private List<SearchTermStat> loadTermStats(
            String eventType, LocalDateTime from, LocalDateTime to, String thesaurusId, int limit
    ) {
        String th = StatJdbc.blankToNull(thesaurusId);
        String sql = """
                SELECT LOWER(TRIM(searched_term)) AS searched_term,
                       thesaurus_id,
                       MAX(thesaurus_label) AS thesaurus_label,
                       COUNT(*) AS nb
                FROM stat_log_event
                WHERE event_type = ?
                  AND event_time >= ? AND event_time < ?
                  AND (? IS NULL OR thesaurus_id = ?)
                GROUP BY LOWER(TRIM(searched_term)), thesaurus_id
                ORDER BY nb DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SearchTermStat(
                        rs.getString("searched_term"),
                        rs.getString("thesaurus_id"),
                        rs.getString("thesaurus_label"),
                        rs.getLong("nb")
                ),
                eventType, StatJdbc.ts(from), StatJdbc.ts(to), th, th, limit
        );
    }

    private void attachLanguages(List<TopConcept> concepts, LocalDateTime from, LocalDateTime to) {
        if (concepts == null || concepts.isEmpty()) {
            return;
        }
        StringBuilder values = new StringBuilder();
        List<Object> params = new ArrayList<>();
        for (int i = 0; i < concepts.size(); i++) {
            if (i > 0) {
                values.append(", ");
            }
            values.append("(?, ?)");
            params.add(concepts.get(i).conceptId());
            params.add(concepts.get(i).thesaurusId());
        }
        params.add(StatJdbc.ts(from));
        params.add(StatJdbc.ts(to));
        String sql = """
                SELECT s.concept_id, s.thesaurus_id, s.lang,
                       MAX(s.concept_label) AS concept_label, COUNT(*) AS nb_vues
                FROM stat_log_event s
                INNER JOIN (VALUES %s) AS c(concept_id, thesaurus_id)
                    ON c.concept_id = s.concept_id AND c.thesaurus_id = s.thesaurus_id
                WHERE s.event_type = 'CONCEPT_VIEW'
                  AND s.event_time >= ? AND s.event_time < ?
                  AND NULLIF(TRIM(s.lang), '') IS NOT NULL
                GROUP BY s.concept_id, s.thesaurus_id, s.lang
                ORDER BY nb_vues DESC
                """.formatted(values);
        Map<String, List<ConceptLangShare>> byKey = new LinkedHashMap<>();
        jdbcTemplate.query(sql, rs -> {
            String key = rs.getString("thesaurus_id") + "|" + rs.getString("concept_id");
            byKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new ConceptLangShare(
                    rs.getString("lang"),
                    rs.getString("concept_label"),
                    rs.getLong("nb_vues")
            ));
        }, params.toArray());
        for (int i = 0; i < concepts.size(); i++) {
            TopConcept concept = concepts.get(i);
            List<ConceptLangShare> langs = byKey.getOrDefault(
                    concept.thesaurusId() + "|" + concept.conceptId(), List.of());
            concepts.set(i, new TopConcept(
                    concept.conceptId(),
                    concept.label(),
                    concept.thesaurusId(),
                    concept.thesaurusLabel(),
                    concept.views(),
                    List.copyOf(langs)
            ));
        }
    }
}
