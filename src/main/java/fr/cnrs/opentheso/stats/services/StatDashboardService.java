package fr.cnrs.opentheso.stats.services;

import fr.cnrs.opentheso.stats.dto.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Requêtes d'agrégation pour le tableau de bord statistique.
 *
 * Règle importante :
 *
 * - thesaurus_id    = identifiant STABLE du thésaurus
 * - thesaurus_label = libellé d'affichage uniquement
 *
 * Toutes les opérations de filtrage et de regroupement utilisent
 * thesaurus_id.
 *
 * Le thesaurus_label est capturé au moment du log et peut évoluer
 * dans le temps. Il ne doit donc jamais être utilisé comme clé
 * statistique.
 */
@Service
public class StatDashboardService {

    private final JdbcTemplate jdbcTemplate;


    public StatDashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    // ============================================================
    // Liste des thésaurus disponibles
    // ============================================================

    /**
     * Liste des thésaurus ayant au moins une donnée statistique.
     *
     * Le regroupement est effectué UNIQUEMENT sur thesaurus_id.
     *
     * Le label est utilisé uniquement pour l'affichage.
     */
    public List<ThesaurusOption> getAvailableThesaurusList() {

        String sql = """
                SELECT
                    thesaurus_id,
                    MAX(thesaurus_label) AS thesaurus_label

                FROM (

                    SELECT
                        thesaurus_id,
                        thesaurus_label

                    FROM stat_log_event

                    WHERE NULLIF(TRIM(thesaurus_id), '') IS NOT NULL


                    UNION ALL


                    SELECT
                        thesaurus_id,
                        thesaurus_label

                    FROM stat_concept_view_daily

                    WHERE NULLIF(TRIM(thesaurus_id), '') IS NOT NULL

                ) combined

                GROUP BY thesaurus_id

                ORDER BY thesaurus_label
                """;


        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new ThesaurusOption(
                        rs.getString("thesaurus_id"),
                        rs.getString("thesaurus_label")
                )
        );
    }


    // ============================================================
    // Liste des langues disponibles
    // ============================================================

    /**
     * Langues effectivement présentes dans les statistiques, pour peupler
     * le sélecteur du dashboard. Trouvée directement dans stat_log_event
     * (seule table à porter la colonne lang).
     */
    public List<String> getAvailableLanguages() {

        String sql = """
                SELECT DISTINCT lang
                FROM stat_log_event
                WHERE NULLIF(TRIM(lang), '') IS NOT NULL
                ORDER BY lang
                """;

        return jdbcTemplate.queryForList(sql, String.class);
    }


    // ============================================================
    // Concepts les plus consultés
    // ============================================================

    /**
     * Retourne les concepts les plus consultés sur une période.
     *
     * Identification :
     *
     *      concept_id + thesaurus_id
     *
     * Le thesaurus_label n'intervient JAMAIS dans le regroupement.
     *
     * Le filtre thésaurus (thesaurusId) porte sur thesaurus_id.
     *
     * La langue (lang, nullable = pas de préférence) sert UNIQUEMENT à
     * choisir quel libellé afficher, jamais à filtrer le nombre de vues :
     * stat_concept_view_daily n'a pas de colonne lang (les vues y sont
     * déjà sommées toutes langues confondues), donc le libellé dans la
     * langue demandée est résolu séparément depuis stat_log_event (seule
     * table à conserver lang), avec repli automatique sur le dernier
     * libellé connu si le concept n'a jamais été vu dans cette langue.
     */
    public List<ConceptStat> getTopConcepts(
            LocalDate from,
            LocalDate to,
            String thesaurusId,
            String lang,
            int limit) {

        String sql = """
            WITH concept_totals AS (

                -- ==================================================
                -- Statistiques agrégées
                -- ==================================================

                SELECT
                    concept_id,
                    thesaurus_id,
                    SUM(total_vues) AS total_vues

                FROM (

                    SELECT
                        concept_id,
                        thesaurus_id,
                        nb_vues AS total_vues

                    FROM stat_concept_view_daily

                    WHERE stat_date BETWEEN ? AND ?

                      AND (
                          NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                          OR thesaurus_id = ?
                      )


                    UNION ALL


                    -- ==================================================
                    -- Événements du jour / non encore agrégés
                    -- ==================================================

                    SELECT
                        concept_id,
                        thesaurus_id,
                        COUNT(*) AS total_vues

                    FROM stat_log_event

                    WHERE event_type = 'CONCEPT_VIEW'

                      AND event_time >= ?
                      AND event_time < ?

                      AND concept_id IS NOT NULL

                      AND (
                          NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                          OR thesaurus_id = ?
                      )

                    GROUP BY
                        concept_id,
                        thesaurus_id

                ) combined

                GROUP BY
                    concept_id,
                    thesaurus_id
            ),


            -- ==========================================================
            -- Dernier label connu dans la langue demandée
            -- ==========================================================

            concept_label_preferred AS (

                SELECT DISTINCT ON (
                    concept_id,
                    thesaurus_id
                )
                    concept_id,
                    thesaurus_id,
                    concept_label,
                    thesaurus_label

                FROM stat_log_event

                WHERE event_type = 'CONCEPT_VIEW'

                  AND concept_label IS NOT NULL

                  AND (
                      NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                      OR lang = ?
                  )

                ORDER BY
                    concept_id,
                    thesaurus_id,
                    event_time DESC
            ),


            -- ==========================================================
            -- Dernier label connu, toutes langues confondues
            -- ==========================================================

            concept_label_fallback AS (

                SELECT DISTINCT ON (
                    concept_id,
                    thesaurus_id
                )
                    concept_id,
                    thesaurus_id,
                    concept_label,
                    thesaurus_label

                FROM stat_log_event

                WHERE event_type = 'CONCEPT_VIEW'

                  AND concept_label IS NOT NULL

                ORDER BY
                    concept_id,
                    thesaurus_id,
                    event_time DESC
            )


            -- ==========================================================
            -- Résultat final
            -- ==========================================================

            SELECT
                t.concept_id,
                t.thesaurus_id,

                COALESCE(
                    pref.concept_label,
                    fb.concept_label,
                    t.concept_id
                ) AS concept_label,

                COALESCE(
                    pref.thesaurus_label,
                    fb.thesaurus_label,
                    t.thesaurus_id
                ) AS thesaurus_label,

                t.total_vues

            FROM concept_totals t

            LEFT JOIN concept_label_preferred pref
                ON pref.concept_id = t.concept_id
               AND pref.thesaurus_id = t.thesaurus_id

            LEFT JOIN concept_label_fallback fb
                ON fb.concept_id = t.concept_id
               AND fb.thesaurus_id = t.thesaurus_id

            ORDER BY
                t.total_vues DESC

            LIMIT ?
            """;


        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new ConceptStat(
                        rs.getString("concept_id"),
                        rs.getString("thesaurus_id"),
                        rs.getString("concept_label"),
                        rs.getString("thesaurus_label"),
                        rs.getLong("total_vues"),
                        0,
                        null,
                        null
                ),

                // concept_totals : stat_concept_view_daily
                from,
                to,
                thesaurusId,
                thesaurusId,

                // concept_totals : stat_log_event
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                thesaurusId,
                thesaurusId,

                // concept_label_preferred
                lang,
                lang,

                // LIMIT
                limit
        );
    }


    // ============================================================
    // Trafic quotidien
    // ============================================================

    /**
     * Retourne le nombre total de consultations par jour.
     *
     * Le filtre éventuel utilise thesaurus_id.
     *
     * Aucun label n'intervient dans les regroupements.
     */
    public List<DailyTrafficStat> getDailyTraffic(
            LocalDate from,
            LocalDate to,
            String thesaurusId) {

        String sql = """
                SELECT
                    stat_date,
                    SUM(total_vues) AS total_vues

                FROM (

                    -- ==========================================
                    -- Données agrégées
                    -- ==========================================

                    SELECT
                        stat_date,
                        nb_vues AS total_vues

                    FROM stat_concept_view_daily

                    WHERE stat_date BETWEEN ? AND ?

                    AND (
                        NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                        OR thesaurus_id = ?
                    )


                    UNION ALL


                    -- ==========================================
                    -- Événements non encore agrégés
                    -- ==========================================

                    SELECT
                        CAST(event_time AS DATE) AS stat_date,
                        COUNT(*) AS total_vues

                    FROM stat_log_event

                    WHERE event_type = 'CONCEPT_VIEW'

                      AND event_time >= ?
                      AND event_time < ?

                      AND concept_id IS NOT NULL

                    AND (
                        NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                        OR thesaurus_id = ?
                    )

                    GROUP BY
                        CAST(event_time AS DATE)

                ) combined

                GROUP BY stat_date

                ORDER BY stat_date
                """;


        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new DailyTrafficStat(
                        rs.getDate("stat_date").toLocalDate(),
                        rs.getLong("total_vues")
                ),

                from,
                to,

                thesaurusId,
                thesaurusId,

                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),

                thesaurusId,
                thesaurusId
        );
    }


    // ============================================================
    // Répartition par thésaurus
    // ============================================================

    /**
     * Retourne le nombre de consultations par thésaurus.
     *
     * IMPORTANT :
     *
     * Le regroupement est effectué UNIQUEMENT sur thesaurus_id.
     *
     * Le thesaurus_label est seulement récupéré pour l'affichage.
     */
    public List<ThesaurusStat> getTrafficByThesaurus(
            LocalDate from,
            LocalDate to) {

        String sql = """
                SELECT
                    thesaurus_id,

                    MAX(thesaurus_label) AS thesaurus_label,

                    SUM(total_vues) AS total_vues

                FROM (

                    -- ==========================================
                    -- Données agrégées
                    -- ==========================================

                    SELECT
                        thesaurus_id,
                        thesaurus_label,
                        nb_vues AS total_vues

                    FROM stat_concept_view_daily

                    WHERE stat_date BETWEEN ? AND ?


                    UNION ALL


                    -- ==========================================
                    -- Événements non encore agrégés
                    -- ==========================================

                    SELECT
                        thesaurus_id,

                        MAX(thesaurus_label) AS thesaurus_label,

                        COUNT(*) AS total_vues

                    FROM stat_log_event

                    WHERE event_type = 'CONCEPT_VIEW'

                      AND event_time >= ?
                      AND event_time < ?
    
                      AND  NULLIF(TRIM(thesaurus_id), '') IS NOT NULL
                      AND concept_id IS NOT NULL

                    GROUP BY thesaurus_id

                ) combined

                GROUP BY thesaurus_id

                ORDER BY total_vues DESC
                """;


        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new ThesaurusStat(
                        rs.getString("thesaurus_id"),
                        rs.getString("thesaurus_label"),
                        rs.getLong("total_vues")
                ),

                from,
                to,

                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()
        );
    }


    // ============================================================
    // Utilisation des API
    // ============================================================

    /**
     * Retourne les endpoints API les plus appelés.
     *
     * Les API ne sont actuellement pas filtrées par thésaurus.
     */
    public List<ApiUsageStat> getApiUsage(
            LocalDateTime from,
            LocalDateTime to) {

        String sql = """
                SELECT
                    url,
                    http_method,
                    COUNT(*) AS nb_appels

                FROM stat_log_event

                WHERE event_type = 'API_CALL'

                  AND event_time >= ?
                  AND event_time < ?

                GROUP BY
                    url,
                    http_method

                ORDER BY nb_appels DESC
                """;


        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new ApiUsageStat(
                        rs.getString("url"),
                        rs.getString("http_method"),
                        rs.getLong("nb_appels")
                ),

                Timestamp.valueOf(from),
                Timestamp.valueOf(to)
        );
    }


    // ============================================================
    // Recherches sans résultat
    // ============================================================

    /**
     * Termes recherchés n'ayant renvoyé aucun résultat.
     *
     * Le filtre utilise thesaurus_id.
     */
    public List<FailedSearchStat> getTopFailedSearches(
            LocalDateTime from,
            LocalDateTime to,
            String thesaurusId,
            int limit) {

        String sql = """
                SELECT
                    LOWER(TRIM(searched_term)) AS searched_term,

                    MAX(thesaurus_label) AS thesaurus_label,
                    thesaurus_id,
                    COUNT(*) AS nb_occurrences

                FROM stat_log_event

                WHERE event_type = 'SEARCH_NO_RESULT'

                  AND event_time >= ?
                  AND event_time < ?

                    AND (
                        NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                        OR thesaurus_id = ?
                    )

                GROUP BY
                    LOWER(TRIM(searched_term)),
                    thesaurus_id

                ORDER BY nb_occurrences DESC

                LIMIT ?
                """;


        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new FailedSearchStat(
                        rs.getString("searched_term"),
                        rs.getString("thesaurus_label"),
                        rs.getString("thesaurus_id"),
                        rs.getLong("nb_occurrences")
                ),

                Timestamp.valueOf(from),
                Timestamp.valueOf(to),

                thesaurusId,
                thesaurusId,

                limit
        );
    }


    // ============================================================
    // Utilisation des synonymes
    // ============================================================

    /**
     * Couples terme recherché -> terme sélectionné.
     *
     * Le filtre utilise thesaurus_id.
     */
    public List<SynonymUsageStat> getTopSynonymUsage(
            LocalDateTime from,
            LocalDateTime to,
            String thesaurusId,
            int limit) {

        String sql = """
                SELECT
                    LOWER(TRIM(searched_term)) AS searched_term,
                    selected_term,
                    thesaurus_label,
                    thesaurus_id,
                    COUNT(*) AS nb_occurrences

                FROM stat_log_event

                WHERE event_type = 'SEARCH_RESULT_SELECTED'

                  AND event_time >= ?
                  AND event_time < ?

                  AND LOWER(TRIM(searched_term))
                      <> LOWER(TRIM(selected_term))

                AND (
                    NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                    OR thesaurus_id = ?
                )

                GROUP BY
                    LOWER(TRIM(searched_term)),
                    selected_term,
                    thesaurus_label,
                    thesaurus_id

                ORDER BY nb_occurrences DESC

                LIMIT ?
                """;


        return jdbcTemplate.query(
                sql,

                (rs, rowNum) -> new SynonymUsageStat(
                        rs.getString("searched_term"),
                        rs.getString("selected_term"),
                        rs.getString("thesaurus_label"),
                        rs.getString("thesaurus_id"),
                        rs.getLong("nb_occurrences")
                ),

                Timestamp.valueOf(from),
                Timestamp.valueOf(to),

                thesaurusId,
                thesaurusId,

                limit
        );
    }


    // ============================================================
    // Recherches globales
    // ============================================================

    /**
     * Recherches globales explicites.
     *
     * Le filtre utilise thesaurus_id.
     */
    public List<GlobalSearchStat> getTopGlobalSearches(
            LocalDateTime from,
            LocalDateTime to,
            String thesaurusId,
            int limit) {

        String sql = """
            SELECT
                LOWER(TRIM(searched_term)) AS searched_term,
                thesaurus_id,
                MAX(thesaurus_label) AS thesaurus_label,
                COUNT(*) AS nb_occurrences
            FROM stat_log_event
            WHERE event_type = 'SEARCH_APPLIED'
              AND event_time >= ?
              AND event_time < ?
              AND NULLIF(TRIM(thesaurus_id), '') IS NOT NULL
              AND (
                  NULLIF(TRIM(CAST(? AS varchar)), '') IS NULL
                  OR thesaurus_id = ?
              )
            GROUP BY
                LOWER(TRIM(searched_term)),
                thesaurus_id
            ORDER BY nb_occurrences DESC
            LIMIT ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new GlobalSearchStat(
                        rs.getString("searched_term"),
                        rs.getString("thesaurus_label"),
                        rs.getString("thesaurus_id"),
                        rs.getLong("nb_occurrences")
                ),
                Timestamp.valueOf(from),
                Timestamp.valueOf(to),

                thesaurusId,
                thesaurusId,

                limit
        );
    }

    public List<ConceptLanguageStat> getConceptLanguageStats(
            LocalDateTime from,
            LocalDateTime to,
            List<ConceptStat> concepts) {

        if (concepts == null || concepts.isEmpty()) {
            return List.of();
        }

        // Construction des couples (concept_id, thesaurus_id)
        StringBuilder values = new StringBuilder();

        for (int i = 0; i < concepts.size(); i++) {

            if (i > 0) {
                values.append(", ");
            }

            values.append("(?, ?)");
        }

        String sql = """
        SELECT
            s.concept_id,
            MAX(s.concept_label) AS concept_label,
            s.thesaurus_id,
            MAX(s.thesaurus_label) AS thesaurus_label,
            s.lang,
            COUNT(*) AS nb_vues

        FROM stat_log_event s

        INNER JOIN (
            VALUES %s
        ) AS c(concept_id, thesaurus_id)
            ON c.concept_id = s.concept_id
           AND c.thesaurus_id = s.thesaurus_id

        WHERE s.event_type = 'CONCEPT_VIEW'
          AND s.event_time >= ?
          AND s.event_time < ?
          AND NULLIF(TRIM(s.lang), '') IS NOT NULL

        GROUP BY
            s.concept_id,
            s.thesaurus_id,
            s.lang

        ORDER BY
            nb_vues DESC
        """.formatted(values);

        List<Object> params = new ArrayList<>();

        // Les concepts à rechercher
        for (ConceptStat concept : concepts) {
            params.add(concept.getConceptId());
            params.add(concept.getThesaurusId());
        }

        // Période
        params.add(Timestamp.valueOf(from));
        params.add(Timestamp.valueOf(to));

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ConceptLanguageStat(
                        rs.getString("concept_id"),
                        rs.getString("concept_label"),
                        rs.getString("thesaurus_id"),
                        rs.getString("thesaurus_label"),
                        rs.getString("lang"),
                        rs.getLong("nb_vues")
                ),
                params.toArray()
        );
    }

}