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







    //////// requêtes direct sur Opentheso BDD /////



    public Double getCoveragePercentageDefinition(String idThesaurus, String sourceLanguage) {
        String sql = """
        SELECT ROUND(
            (SELECT COUNT(identifier) 
             FROM note 
             WHERE id_thesaurus = ? 
               AND lang = ? 
               AND notetypecode = 'definition') * 100.0 / 
            NULLIF((SELECT COUNT(DISTINCT id_concept) 
                    FROM concept 
                    WHERE id_thesaurus = ?), 0), 
        2)
        """;

        return jdbcTemplate.queryForObject(sql, Double.class, idThesaurus, sourceLanguage, idThesaurus);
    }

    public Double getCoveragePercentageTraduction(String idThesaurus, String sourceLanguage) {
        String sql = """
            SELECT
                ROUND(
                    100.0 * COUNT(*) FILTER (
                        WHERE nb_langues_secondaires > 0
                    ) / NULLIF(COUNT(*), 0),
                    2
                ) AS score_percent

            FROM (
                SELECT
                    pt.id_concept,

                    COUNT(DISTINCT t.lang) FILTER (
                        WHERE LOWER(TRIM(t.lang)) <> LOWER(TRIM(?))
                    ) AS nb_langues_secondaires

                FROM preferred_term pt

                JOIN term t
                    ON t.id_term = pt.id_term
                    AND t.id_thesaurus = pt.id_thesaurus

                WHERE pt.id_thesaurus = ?

                GROUP BY pt.id_concept

                HAVING COUNT(*) FILTER (
                    WHERE LOWER(TRIM(t.lang)) = LOWER(TRIM(?))
                ) > 0

            ) stats
            """;

        return jdbcTemplate.queryForObject(sql, Double.class, sourceLanguage, idThesaurus, sourceLanguage);
    }

    //////// Fin des requêtes direct sur Opentheso BDD /////


    // ============================================================
    // Couverture linguistique
    // ============================================================

    /**
     * Distribution du nombre de concepts par nombre de langues dans
     * lesquelles ils disposent d'un terme préférentiel.
     *
     * Exemple de résultat :
     *   1 langue  -> 430 concepts
     *   2 langues -> 510 concepts
     *   3 langues -> 220 concepts
     *
     * La moyenne "langues / concept" se calcule à partir de cette
     * distribution côté appelant (pas besoin d'une deuxième requête) :
     *   moyenne = SUM(nbLangues * nbConcepts) / SUM(nbConcepts)
     */
    public List<LanguageCoverageStat> getLanguageCoverageDistribution(String idThesaurus) {

        String sql = """
                SELECT
                    nb_langues,
                    COUNT(*) AS nb_concepts
 
                FROM (
 
                    SELECT
                        c.id_concept,
                        COUNT(DISTINCT t.lang) AS nb_langues
 
                    FROM concept c
 
                    LEFT JOIN preferred_term pt
                        ON pt.id_concept = c.id_concept
                       AND pt.id_thesaurus = c.id_thesaurus
 
                    LEFT JOIN term t
                        ON t.id_term = pt.id_term
                       AND t.id_thesaurus = pt.id_thesaurus
 
                    WHERE c.id_thesaurus = ?
                      AND (c.status IS NULL OR c.status NOT IN ('CA', 'DEP'))
 
                    GROUP BY c.id_concept
 
                ) per_concept
 
                GROUP BY nb_langues
 
                ORDER BY nb_langues
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new LanguageCoverageStat(
                rs.getInt("nb_langues"),
                rs.getLong("nb_concepts")
        ), idThesaurus);
    }


    /**
     * Liste détaillée des concepts ayant EXACTEMENT nbLangues langues
     * renseignées, pour un thésaurus donné — utilisée par la popup
     * "cliquer sur un palier de couverture" et son export CSV.
     *
     * ATTENTION : "t.lexical_value" est une supposition sur le nom de la
     * colonne portant le libellé dans votre table "term" — remplacez-la
     * par le nom réel de cette colonne dans votre schéma.
     */
    public List<ConceptToTranslate> getConceptsByLanguageCoverage(
            String idThesaurus,
            int nbLangues,
            String sourceLang) {

        String sql = """
                SELECT
                    c.id_concept,
 
                    MAX(CASE WHEN LOWER(t.lang) = LOWER(?) THEN t.lexical_value END) AS label,
 
                    STRING_AGG(DISTINCT UPPER(t.lang), ', ' ORDER BY UPPER(t.lang)) AS existing_langs
 
                FROM concept c
 
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                   AND pt.id_thesaurus = c.id_thesaurus
 
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                   AND t.id_thesaurus = pt.id_thesaurus
 
                WHERE c.id_thesaurus = ?
                  AND (c.status IS NULL OR c.status NOT IN ('CA', 'DEP'))
 
                GROUP BY c.id_concept
 
                HAVING COUNT(DISTINCT t.lang) = ?
 
                ORDER BY label NULLS FIRST
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ConceptToTranslate(
                rs.getString("id_concept"),
                rs.getString("label"),
                rs.getString("existing_langs")
        ), sourceLang, idThesaurus, nbLangues);
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



    /**
     * Calcule le pourcentage de concepts possédant au moins une définition
     * dans une langue secondaire par rapport à la langue source du thésaurus.
     *
     * @param thesaurusId identifiant du thésaurus
     * @param sourceLang langue principale/source du thésaurus (ex: "fr")
     * @return pourcentage de concepts ayant une définition traduite, de 0 à 100
     */
    public double getTranslatedDefinitionsScore(
            String thesaurusId,
            String sourceLang) {

        String sql = """
            SELECT
                COUNT(DISTINCT c.id_concept) AS total_concepts,

                COUNT(DISTINCT CASE
                    WHEN n.identifier IS NOT NULL
                    THEN c.id_concept
                END) AS concepts_avec_definition_traduite

            FROM concept c

            LEFT JOIN note n
                   ON n.identifier = c.id_concept
                  AND n.id_thesaurus = c.id_thesaurus
                  AND n.notetypecode = 'definition'
                  AND NULLIF(TRIM(n.lang), '') IS NOT NULL
                  AND LOWER(TRIM(n.lang)) <> LOWER(TRIM(?))

            WHERE c.id_thesaurus = ?
            """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {
                    long totalConcepts = rs.getLong("total_concepts");
                    long translatedDefinitions =
                            rs.getLong("concepts_avec_definition_traduite");

                    if (totalConcepts == 0) {
                        return 0.0;
                    }

                    return (translatedDefinitions * 100.0) / totalConcepts;
                },
                sourceLang,
                thesaurusId
        );
    }


    /**
     * Calcule le pourcentage de concepts possédant au moins un alignement
     * dans un thésaurus.
     *
     * @param thesaurusId identifiant du thésaurus
     * @return pourcentage de concepts ayant au moins un alignement, de 0 à 100
     */
    public double getAlignmentCoverageScore(String thesaurusId) {

        String sql = """
            SELECT
                COUNT(DISTINCT c.id_concept) AS total_concepts,

                COUNT(DISTINCT CASE
                    WHEN a.internal_id_concept IS NOT NULL
                    THEN c.id_concept
                END) AS concepts_avec_alignement

            FROM concept c

            LEFT JOIN alignement a
                   ON a.internal_id_concept = c.id_concept
                  AND a.internal_id_thesaurus = c.id_thesaurus

            WHERE c.id_thesaurus = ?
            """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    long totalConcepts =
                            rs.getLong("total_concepts");

                    long conceptsAvecAlignement =
                            rs.getLong("concepts_avec_alignement");

                    if (totalConcepts == 0) {
                        return 0.0;
                    }

                    return conceptsAvecAlignement * 100.0 / totalConcepts;
                },
                thesaurusId
        );
    }

    /**
     * Calcule le pourcentage de concepts possédant un identifiant ARK
     * dans un thésaurus.
     *
     * @param thesaurusId identifiant du thésaurus
     * @return pourcentage de concepts ayant un ARK, de 0 à 100
     */
    public double getArkCoverageScore(String thesaurusId) {

        String sql = """
            SELECT
                COUNT(*) AS total_concepts,

                COUNT(*) FILTER (
                    WHERE NULLIF(TRIM(id_ark), '') IS NOT NULL
                ) AS concepts_avec_ark

            FROM concept

            WHERE id_thesaurus = ?
            """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    long totalConcepts =
                            rs.getLong("total_concepts");

                    long conceptsAvecArk =
                            rs.getLong("concepts_avec_ark");

                    if (totalConcepts == 0) {
                        return 0.0;
                    }

                    return conceptsAvecArk * 100.0 / totalConcepts;
                },
                thesaurusId
        );
    }



    public double getRtCoverageScore(String thesaurusId) {

        String sql = """
        SELECT
            COUNT(*) AS total_concepts,

            COUNT(*) FILTER (
                WHERE EXISTS (
                    SELECT 1
                    FROM hierarchical_relationship hr
                    WHERE hr.id_thesaurus = c.id_thesaurus
                      AND hr.role = 'RT'
                      AND (
                          hr.id_concept1 = c.id_concept
                          OR hr.id_concept2 = c.id_concept
                      )
                )
            ) AS concepts_avec_rt

        FROM concept c

        WHERE c.id_thesaurus = ?
        """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    long totalConcepts =
                            rs.getLong("total_concepts");

                    long conceptsAvecRt =
                            rs.getLong("concepts_avec_rt");

                    if (totalConcepts == 0) {
                        return 0.0;
                    }

                    return conceptsAvecRt * 100.0 / totalConcepts;
                },
                thesaurusId
        );
    }



    public double getHierarchyScore(String thesaurusId) {

        String sql = """
        SELECT
            COUNT(*) AS total_concepts,

            COUNT(*) FILTER (
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM hierarchical_relationship hr
                    WHERE hr.id_thesaurus = c.id_thesaurus
                      AND hr.role IN ('BT', 'NT')
                      AND hr.id_concept1 = c.id_concept
                )
            ) AS concepts_sans_bt_nt

        FROM concept c

        WHERE c.id_thesaurus = ?
          AND (c.status IS NULL OR c.status NOT IN ('CA', 'DEP'))
        """;

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {

                    long totalConcepts =
                            rs.getLong("total_concepts");

                    long conceptsSansBtNt =
                            rs.getLong("concepts_sans_bt_nt");

                    if (totalConcepts == 0) {
                        return 0.0;
                    }

                    double conceptsWithoutBtNtRate =
                            conceptsSansBtNt * 100.0 / totalConcepts;

                    return 100.0 - conceptsWithoutBtNtRate;
                },
                thesaurusId
        );
    }


    // ============================================================
    // Couverture des définitions par langue
    // ============================================================

    /**
     * Pour chaque langue du thésaurus : combien de concepts actifs
     * disposent d'une définition (note de type definitionTypeCode) dans
     * cette langue, et le taux de couverture correspondant.
     *
     * Les langues proviennent de la table term (toutes les langues
     * effectivement utilisées dans le thésaurus), pas seulement celles
     * ayant déjà une définition — une langue à 0% doit apparaître, pas
     * disparaître.
     *
     * ATTENTION : "definitionTypeCode" doit correspondre à la valeur
     * réelle de note.notetypecode identifiant une définition dans votre
     * schéma (vérifiable via : SELECT DISTINCT notetypecode FROM note;).
     */
    public List<DefinitionCoverageStat> getDefinitionCoverageByLanguage(
            String idThesaurus,
            String definitionTypeCode) {

        String sql = """
                WITH langs AS (
                    SELECT DISTINCT lang
                    FROM term
                    WHERE id_thesaurus = ?
                ),
 
                active_concepts AS (
                    SELECT id_concept
                    FROM concept
                    WHERE id_thesaurus = ?
                      AND (status IS NULL OR status NOT IN ('CA', 'DEP'))
                ),
 
                total AS (
                    SELECT COUNT(*) AS total_concepts
                    FROM active_concepts
                )
 
                SELECT
                    l.lang,
                    COUNT(DISTINCT n.identifier) AS nb_concepts_with_definition,
                    t.total_concepts,
                    ROUND(
                        100.0 * COUNT(DISTINCT n.identifier) / NULLIF(t.total_concepts, 0),
                        2
                    ) AS coverage_percent
 
                FROM langs l
 
                CROSS JOIN total t
 
                LEFT JOIN note n
                    ON n.id_thesaurus = ?
                   AND n.notetypecode = ?
                   AND LOWER(n.lang) = LOWER(l.lang)
                   AND n.identifier IN (SELECT id_concept FROM active_concepts)
 
                GROUP BY l.lang, t.total_concepts
 
                ORDER BY coverage_percent DESC NULLS LAST
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            java.math.BigDecimal coveragePercent = rs.getBigDecimal("coverage_percent");
            return new DefinitionCoverageStat(
                    rs.getString("lang"),
                    rs.getLong("nb_concepts_with_definition"),
                    rs.getLong("total_concepts"),
                    (coveragePercent != null) ? coveragePercent.doubleValue() : null
            );
        }, idThesaurus, idThesaurus, idThesaurus, definitionTypeCode);
    }


    /**
     * Concepts actifs n'ayant PAS de définition dans la langue donnée
     * (popup de détail au clic sur une ligne du tableau ci-dessus).
     * Le libellé affiché est celui de la langue source, pour identifier
     * facilement quel concept reste à documenter.
     */
    public List<ConceptMissingDefinition> getConceptsMissingDefinition(
            String idThesaurus,
            String lang,
            String definitionTypeCode,
            String sourceLang) {

        String sql = """
                SELECT
                    c.id_concept,
                    MAX(CASE WHEN LOWER(t.lang) = LOWER(?) THEN t.lexical_value END) AS label
 
                FROM concept c
 
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                   AND pt.id_thesaurus = c.id_thesaurus
 
                LEFT JOIN term t
                    ON t.id_term = pt.id_term
                   AND t.id_thesaurus = pt.id_thesaurus
 
                WHERE c.id_thesaurus = ?
                  AND (c.status IS NULL OR c.status NOT IN ('CA', 'DEP'))
 
                  AND NOT EXISTS (
                      SELECT 1
                      FROM note n
                      WHERE n.identifier = c.id_concept
                        AND n.id_thesaurus = c.id_thesaurus
                        AND n.notetypecode = ?
                        AND LOWER(n.lang) = LOWER(?)
                  )
 
                GROUP BY c.id_concept
 
                ORDER BY label NULLS FIRST
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ConceptMissingDefinition(
                rs.getString("id_concept"),
                rs.getString("label")
        ), sourceLang, idThesaurus, definitionTypeCode, lang);
    }



}