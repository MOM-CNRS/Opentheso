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
 * (conservée indéfiniment, petite table, donc rapide même sur "depuis toujours").
 * Les stats d'API s'appuient sur stat_log_event (détail brut, purgé après 1 an :
 * ne fonctionne donc que sur les 12 derniers mois glissants).
 */
@Service
public class StatDashboardService {

    private final JdbcTemplate jdbcTemplate;

    public StatDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Concepts les plus consultés sur la période (base du nuage de mots).
     * Le libellé n'est pas renseigné ici : à compléter par l'appelant via
     * le service de concepts existant, à partir de conceptId + thesaurusIdt.
     */
    public List<ConceptStat> getTopConcepts(LocalDate from, LocalDate to, int limit) {
        String sql = """
                SELECT concept_id, thesaurus_id, SUM(nb_vues) AS total_vues
                FROM stat_concept_view_daily
                WHERE stat_date BETWEEN ? AND ?
                GROUP BY concept_id, thesaurus_id
                ORDER BY total_vues DESC
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ConceptStat(
                rs.getString("concept_id"),
                rs.getString("thesaurus_id"),
                rs.getLong("total_vues")
        ), from, to, limit);
    }

    /**
     * Trafic quotidien total (toutes vues de concepts confondues), pour le
     * graphique de tendance.
     */
    public List<DailyTrafficStat> getDailyTraffic(LocalDate from, LocalDate to) {
        String sql = """
                SELECT stat_date, SUM(nb_vues) AS total_vues
                FROM stat_concept_view_daily
                WHERE stat_date BETWEEN ? AND ?
                GROUP BY stat_date
                ORDER BY stat_date
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new DailyTrafficStat(
                rs.getDate("stat_date").toLocalDate(),
                rs.getLong("total_vues")
        ), from, to);
    }

    /**
     * Répartition des consultations par thésaurus sur la période.
     */
    public List<ThesaurusStat> getTrafficByThesaurus(LocalDate from, LocalDate to) {
        String sql = """
                SELECT thesaurus_id, SUM(nb_vues) AS total_vues
                FROM stat_concept_view_daily
                WHERE stat_date BETWEEN ? AND ?
                GROUP BY thesaurus_id
                ORDER BY total_vues DESC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ThesaurusStat(
                rs.getString("thesaurus_id"),
                rs.getLong("total_vues")
        ), from, to);
    }

    /**
     * Endpoints API les plus appelés sur la période.
     * Repose sur stat_log_event : ne couvre que les 12 derniers mois glissants
     * (purge automatique au-delà).
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
