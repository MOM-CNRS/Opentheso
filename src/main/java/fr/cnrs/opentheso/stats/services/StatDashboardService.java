package fr.cnrs.opentheso.stats.services;

import fr.cnrs.opentheso.stats.dto.ApiUsageStat;
import fr.cnrs.opentheso.stats.dto.ConceptStat;
import fr.cnrs.opentheso.stats.dto.DailyTrafficStat;
import fr.cnrs.opentheso.stats.dto.ThesaurusStat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Requêtes d'agrégation pour le tableau de bord statistique.
 *
 * Les stats "concept" et "thésaurus" s'appuient sur stat_concept_view_daily
 * (conservée indéfiniment), combinée en temps réel avec les événements du
 * jour même issus de stat_log_event (non encore agrégés par le job nocturne).
 *
 * thesaurus_label est stocké directement dans les deux tables : contrairement
 * au libellé de concept (qui change souvent et doit être résolu via le
 * service de concepts pour rester à jour), le nom d'un thésaurus varie très
 * rarement, donc pas besoin de résolution externe ici.
 */
@Service
public class StatDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public StatDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Concepts les plus consultés sur la période (base du nuage de mots).
     * Le libellé du CONCEPT n'est pas renseigné ici : à compléter par
     * l'appelant via le service de concepts existant (voir ConceptStat).
     */
    public List<ConceptStat> getTopConcepts(LocalDate from, LocalDate to, int limit) {
        String sql = """
                SELECT concept_id, thesaurus_id, MAX(thesaurus_label) AS thesaurus_label,
                       SUM(total_vues) AS total_vues
                FROM (
                    SELECT concept_id, thesaurus_id, thesaurus_label, nb_vues AS total_vues
                    FROM stat_concept_view_daily
                    WHERE stat_date BETWEEN ? AND ?

                    UNION ALL

                    SELECT concept_id, thesaurus_id, MAX(thesaurus_label) AS thesaurus_label,
                           COUNT(*) AS total_vues
                    FROM stat_log_event
                    WHERE event_type = 'CONCEPT_VIEW'
                      AND event_time >= current_date
                      AND concept_id IS NOT NULL
                    GROUP BY concept_id, thesaurus_id
                ) combined
                GROUP BY concept_id, thesaurus_id
                ORDER BY total_vues DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ConceptStat(
                rs.getString("concept_id"),
                rs.getString("thesaurus_id"),
                rs.getString("thesaurus_label"),
                rs.getLong("total_vues")
        ), from, to, limit);
    }

    /**
     * Trafic quotidien total (toutes vues de concepts confondues).
     * Inclut le jour même calculé en temps réel.
     */
    public List<DailyTrafficStat> getDailyTraffic(LocalDate from, LocalDate to) {
        String sql = """
                SELECT stat_date, SUM(total_vues) AS total_vues
                FROM (
                    SELECT stat_date, nb_vues AS total_vues
                    FROM stat_concept_view_daily
                    WHERE stat_date BETWEEN ? AND ?

                    UNION ALL

                    SELECT current_date AS stat_date, COUNT(*) AS total_vues
                    FROM stat_log_event
                    WHERE event_type = 'CONCEPT_VIEW'
                      AND event_time >= current_date
                      AND concept_id IS NOT NULL
                ) combined
                GROUP BY stat_date
                ORDER BY stat_date
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DailyTrafficStat(
                rs.getDate("stat_date").toLocalDate(),
                rs.getLong("total_vues")
        ), from, to);
    }

    /**
     * Répartition des consultations par thésaurus sur la période, avec libellé.
     * Inclut le jour même calculé en temps réel.
     */
    public List<ThesaurusStat> getTrafficByThesaurus(LocalDate from, LocalDate to) {
        String sql = """
                SELECT thesaurus_id, MAX(thesaurus_label) AS thesaurus_label,
                       SUM(total_vues) AS total_vues
                FROM (
                    SELECT thesaurus_id, thesaurus_label, nb_vues AS total_vues
                    FROM stat_concept_view_daily
                    WHERE stat_date BETWEEN ? AND ?

                    UNION ALL

                    SELECT thesaurus_id, MAX(thesaurus_label) AS thesaurus_label,
                           COUNT(*) AS total_vues
                    FROM stat_log_event
                    WHERE event_type = 'CONCEPT_VIEW'
                      AND event_time >= current_date
                      AND concept_id IS NOT NULL
                    GROUP BY thesaurus_id
                ) combined
                GROUP BY thesaurus_id
                ORDER BY total_vues DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ThesaurusStat(
                rs.getString("thesaurus_id"),
                rs.getString("thesaurus_label"),
                rs.getLong("total_vues")
        ), from, to);
    }

    /**
     * Endpoints API les plus appelés sur la période (inclut nativement aujourd'hui).
     */
    public List<ApiUsageStat> getApiUsage(LocalDateTime from, LocalDateTime to, int limit) {
        String sql = """
                SELECT url, http_method, COUNT(*) AS nb_appels
                FROM stat_log_event
                WHERE event_type = 'API_CALL'
                  AND event_time BETWEEN ? AND ?
                GROUP BY url, http_method
                ORDER BY nb_appels DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ApiUsageStat(
                rs.getString("url"),
                rs.getString("http_method"),
                rs.getLong("nb_appels")
        ), Timestamp.valueOf(from), Timestamp.valueOf(to), limit);
    }
}