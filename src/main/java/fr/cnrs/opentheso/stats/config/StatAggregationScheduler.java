package fr.cnrs.opentheso.stats.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StatAggregationScheduler {

    private static final Logger log = LoggerFactory.getLogger(StatAggregationScheduler.class);

    private static final String AGGREGATE_YESTERDAY_SQL = """
            INSERT INTO stat_concept_view_daily
                (stat_date, concept_id, thesaurus_id, thesaurus_label, nb_vues)
            SELECT date_trunc('day', event_time)::date, concept_id, thesaurus_id,
                   MAX(thesaurus_label), COUNT(*)
            FROM stat_log_event
            WHERE event_type = 'CONCEPT_VIEW'
              AND event_time >= current_date - interval '1 day'
              AND event_time < current_date
              AND concept_id IS NOT NULL
            GROUP BY 1, concept_id, thesaurus_id
            ON CONFLICT (stat_date, concept_id, thesaurus_id)
            DO UPDATE SET nb_vues = EXCLUDED.nb_vues,
                          thesaurus_label = EXCLUDED.thesaurus_label
            """;

    private static final String PURGE_OLD_EVENTS_SQL = """
            DELETE FROM stat_log_event
            WHERE event_time < now() - interval '1 year'
            """;

    private final JdbcTemplate jdbcTemplate;

    public StatAggregationScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void aggregateYesterdayConceptViews() {
        try {
            int rows = jdbcTemplate.update(AGGREGATE_YESTERDAY_SQL);
            log.info("Agrégation quotidienne stat_concept_view_daily : {} lignes mises à jour.", rows);
        } catch (Exception e) {
            log.error("Échec de l'agrégation quotidienne des statistiques.", e);
        }
    }

    @Scheduled(cron = "0 0 3 1 * *")
    public void purgeOldRawEvents() {
        try {
            int deleted = jdbcTemplate.update(PURGE_OLD_EVENTS_SQL);
            log.info("Purge stat_log_event : {} lignes supprimées (plus d'un an).", deleted);
        } catch (Exception e) {
            log.error("Échec de la purge des événements statistiques.", e);
        }
    }
}