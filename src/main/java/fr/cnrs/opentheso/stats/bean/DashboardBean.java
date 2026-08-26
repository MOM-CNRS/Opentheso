package fr.cnrs.opentheso.stats.bean;

import fr.cnrs.opentheso.stats.dto.*;
import fr.cnrs.opentheso.stats.services.StatDashboardService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import lombok.Data;

import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.charts.PieChart;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.data.PieData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.dataset.PieDataset;
import software.xdev.chartjs.model.options.elements.Fill;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Bean JSF du tableau de bord statistique Opentheso.
 *
 * Les statistiques sont basées sur les identifiants stables :
 *
 * - concept_id
 * - thesaurus_id
 *
 * Les labels sont récupérés depuis les données statistiques
 * enregistrées dans stat_log_event.
 *
 * La langue sélectionnée sert uniquement à déterminer le label
 * à afficher. Elle ne modifie pas le nombre de consultations.
 *
 * Le dashboard peut être filtré par :
 *
 * - période
 * - thésaurus
 * - langue d'affichage
 *
 * Les graphiques utilisent chartjs-java-model
 * (software.xdev) et sont sérialisés en JSON pour PrimeFaces.
 */
@Data
@Named
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final int TOP_CONCEPTS_LIMIT = 50;
    private static final int TOP_SEARCHES_LIMIT = 30;

    private static final int MIN_WORD_CLOUD_FONT_SIZE = 14;
    private static final int MAX_WORD_CLOUD_FONT_SIZE = 46;

    private static final DateTimeFormatter CHART_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM");

    private List<ConceptLanguageStat> selectedConceptLanguageStats;
    private ConceptStat selectedConcept;

    @Inject
    private StatDashboardService statDashboardService;


    // ============================================================
    // Période
    // ============================================================

    public enum Period {

        LAST_7_DAYS("7 derniers jours", 7),
        LAST_30_DAYS("30 derniers jours", 30),
        LAST_3_MONTHS("3 derniers mois", 90),
        LAST_YEAR("Dernière année", 365);

        private final String label;
        private final long days;

        Period(String label, long days) {
            this.label = label;
            this.days = days;
        }

        public String getLabel() {
            return label;
        }

        public long getDays() {
            return days;
        }
    }

    private Period selectedPeriod = Period.LAST_30_DAYS;


    // ============================================================
    // Langue
    // ============================================================

    /**
     * Langue utilisée pour afficher les labels des concepts
     * et des thésaurus.
     *
     * Exemple :
     *
     * fr -> Maison
     * en -> House
     *
     * null ou vide = pas de préférence (dernier libellé connu,
     * toutes langues confondues).
     */
    private String selectedLanguage;

    /**
     * Langues disponibles dans le dashboard.
     */
    private List<LanguageOption> availableLanguages;


    // ============================================================
    // Filtre thésaurus
    // ============================================================

    /**
     * Identifiant du thésaurus sélectionné.
     *
     * null ou vide = tous les thésaurus.
     */
    private String selectedThesaurusId;

    /**
     * Liste des thésaurus disponibles.
     */
    private List<ThesaurusOption> availableThesaurusList;


    // ============================================================
    // Concepts / nuage de mots
    // ============================================================

    /**
     * Concepts les plus consultés.
     */
    private List<ConceptStat> topConcepts;

    /**
     * Données préparées pour le nuage de mots.
     */
    private List<WordCloudEntry> wordCloudEntries;



    // ============================================================
    // Graphiques
    // ============================================================

    /**
     * Trafic quotidien.
     */
    private String dailyTrafficChartJson;

    /**
     * Répartition des consultations par thésaurus.
     */
    private String thesaurusPieChartJson;

    /**
     * Utilisation des endpoints API.
     */
    private String apiUsageChartJson;


    // ============================================================
    // Statistiques API
    // ============================================================

    /**
     * Endpoints API les plus utilisés.
     */
    private List<ApiUsageStat> apiUsageStats;


    // ============================================================
    // Statistiques de recherche
    // ============================================================

    /**
     * Recherches n'ayant retourné aucun résultat.
     */
    private List<FailedSearchStat> topFailedSearches;

    /**
     * Utilisation des synonymes.
     */
    private List<SynonymUsageStat> topSynonymUsage;

    /**
     * Recherches globales.
     */
    private List<GlobalSearchStat> topGlobalSearches;


    // ============================================================
    // Initialisation
    // ============================================================

    @PostConstruct
    public void init() {

        loadAvailableThesaurus();

        loadAvailableLanguages();

        refreshDashboard();
    }


    private ThesaurusQualityScore qualityScore;
    private void loadQualityScore() {

        if (!isThesaurusFilterActive()) {
            qualityScore = null;
            return;
        }

     /* ==========================================================
     *
     * Ces valeurs seront remplacées ensuite par les résultats
     * des requêtes PostgreSQL.
                */

        double definitionsScore = 74.0;

        double translatedTermsScore = 48.0;

        double translatedDefinitionsScore = 31.0;

        double alignmentsScore = 22.0;

        double arkScore = 90.0;

        /*
         * % de concepts possédant au moins une relation RT.
         */
        double rtScore = 65.0;

        /*
         * % de concepts n'ayant AUCUNE relation BT ou NT.
         *
         * Exemple :
         *
         * 18 % des concepts sans BT/NT
         *
         * => score qualité = 82 %
         */
        double conceptsWithoutBtNtRate = 18.0;

        double hierarchyScore = 100.0 - conceptsWithoutBtNtRate;

        /*
         * Ancienneté de la dernière modification.
         *
         * Exemple : dernière modification il y a 8 mois.
         */
        double lastModificationScore = 85.0;
     /*   double lastModificationScore =
                calculateModificationScore(lastModificationScore2);
*/

        // TODO : remplacer ces valeurs figées par de vraies requêtes, par ex. :
        //   - COUNT(concepts avec au moins une définition) / COUNT(total concepts)
        //   - COUNT(concepts avec un terme traduit dans une langue secondaire) / COUNT(total concepts)
        //   - COUNT(concepts avec une définition traduite) / COUNT(total concepts)
        //   - COUNT(concepts avec au moins un alignement) / COUNT(total concepts)
        //   - COUNT(concepts avec un identifiant ARK) / COUNT(total concepts)
    /*    List<QualityCriterion> criteria = List.of(
                new QualityCriterion("Concepts avec une définition", 74.0, 0.25),
                new QualityCriterion("Termes traduits (langues secondaires)", 48.0, 0.20),
                new QualityCriterion("Définitions traduites", 31.0, 0.15),
                new QualityCriterion("Concepts alignés (autres thésaurus)", 22.0, 0.20),
                new QualityCriterion("Identifiants pérennes (ARK)", 90.0, 0.20)
        );*/

        List<QualityCriterion> criteria = List.of(

                new QualityCriterion(
                        "Concepts avec une définition",
                        definitionsScore,
                        0.15
                ),

                new QualityCriterion(
                        "Termes traduits (langues secondaires)",
                        translatedTermsScore,
                        0.10
                ),

                new QualityCriterion(
                        "Définitions traduites",
                        translatedDefinitionsScore,
                        0.10
                ),

                new QualityCriterion(
                        "Concepts alignés (autres thésaurus)",
                        alignmentsScore,
                        0.10
                ),

                new QualityCriterion(
                        "Identifiants pérennes (ARK)",
                        arkScore,
                        0.10
                ),

                new QualityCriterion(
                        "Concepts avec relation associative (RT)",
                        rtScore,
                        0.10
                ),

                new QualityCriterion(
                        "Concepts avec relation hiérarchique (BT/NT)",
                        hierarchyScore,
                        0.20
                ),

                new QualityCriterion(
                        "Fraîcheur du thésaurus",
                        lastModificationScore,
                        0.15
                )
        );



        double overall = criteria.stream()
                .mapToDouble(QualityCriterion::getContribution)
                .sum();

        qualityScore = new ThesaurusQualityScore(overall, criteria);
    }

    private double calculateModificationScore(LocalDate lastModification) {

        if (lastModification == null) {
            return 0;
        }

        long months = java.time.temporal.ChronoUnit.MONTHS.between(
                lastModification,
                LocalDate.now()
        );

        if (months < 3) {
            return 100;
        }

        if (months < 6) {
            return 95;
        }

        if (months < 12) {
            return 85;
        }

        if (months < 24) {
            return 70;
        }

        if (months < 36) {
            return 50;
        }

        if (months < 60) {
            return 30;
        }

        return 0;
    }



    /**
     * Angle de rotation (en degrés) de l'aiguille de la jauge SVG.
     * -90° = tout à gauche (score 0), 0° = vertical (score 50), +90° =
     * tout à droite (score 100).
     */
    public double getGaugeNeedleRotation() {

        double score = (qualityScore != null) ? qualityScore.getOverallScore() : 0.0;

        return (score / 100.0) * 180.0 - 90.0;
    }



    // ============================================================
    // Listes de filtres
    // ============================================================

    private void loadAvailableThesaurus() {

        availableThesaurusList =
                statDashboardService.getAvailableThesaurusList();
    }


    /**
     * Codes de langue affichables en toutes lettres, pour un rendu plus
     * lisible dans le sélecteur. Repli sur le code brut (en majuscules)
     * si une langue rencontrée en base n'est pas dans cette table.
     */
    private static final java.util.Map<String, String> LANGUAGE_DISPLAY_NAMES = java.util.Map.of(
            "fr", "Français",
            "en", "English",
            "es", "Español",
            "de", "Deutsch",
            "it", "Italiano"
    );

    /**
     * Charge les langues réellement présentes dans les statistiques,
     * trouvées directement dans stat_log_event (seule table à porter
     * la colonne lang). Remplace l'ancienne liste statique.
     */
    private void loadAvailableLanguages() {

        List<String> codes = statDashboardService.getAvailableLanguages();

        availableLanguages = codes.stream()
                .map(code -> new LanguageOption(
                        code,
                        LANGUAGE_DISPLAY_NAMES.getOrDefault(code, code.toUpperCase())
                ))
                .toList();
    }


    /**
     * Liste utilisée par le selectOneMenu des périodes.
     */
    public Period[] getAvailablePeriods() {

        return Period.values();
    }


    /**
     * Indique si un filtre thésaurus est actif.
     */
    public boolean isThesaurusFilterActive() {

        return selectedThesaurusId != null
                && !selectedThesaurusId.isBlank();
    }


    /**
     * Indique si une langue précise est sélectionnée (utile côté page,
     * même principe que isThesaurusFilterActive()).
     */
    public boolean isLanguageFilterActive() {

        return selectedLanguage != null
                && !selectedLanguage.isBlank();
    }


    // ============================================================
    // Actions AJAX
    // ============================================================

    /**
     * Rafraîchissement manuel.
     */
    public void refreshNow() {

        refreshDashboard();
    }


    /**
     * Changement de période.
     */
    public void onPeriodChange() {

        refreshDashboard();
    }


    /**
     * Changement de thésaurus.
     */
    public void onThesaurusChange() {

        refreshDashboard();
    }


    /**
     * Changement de langue.
     *
     * IMPORTANT :
     *
     * La langue ne modifie pas les statistiques.
     * Elle modifie uniquement les labels affichés.
     */
    public void onLanguageChange() {

        refreshDashboard();
    }


    // ============================================================
    // Rafraîchissement général
    // ============================================================

    /**
     * Recharge l'ensemble des statistiques.
     */
    public void refreshDashboard() {

        LocalDate from = getFromDate();
        LocalDate to = getToDate();

        LocalDateTime fromDateTime =
                from.atStartOfDay();

        LocalDateTime toDateTime =
                to.plusDays(1).atStartOfDay();


        // --------------------------------------------------------
        // Concepts / nuage de mots
        // --------------------------------------------------------

        loadTopConcepts(from, to);


        // --------------------------------------------------------
        // Trafic quotidien
        // --------------------------------------------------------

        loadDailyTrafficChart(from, to);


        // --------------------------------------------------------
        // Répartition par thésaurus
        // --------------------------------------------------------

        loadThesaurusPieChart(from, to);


        // --------------------------------------------------------
        // API
        // --------------------------------------------------------

        loadApiUsageChart(
                fromDateTime,
                toDateTime
        );

        apiUsageStats =
                statDashboardService.getApiUsage(
                        fromDateTime,
                        toDateTime
                );


        // --------------------------------------------------------
        // Recherches
        // --------------------------------------------------------

        loadSearchStats(
                fromDateTime,
                toDateTime
        );


        // --------------------------------------------------------
        // Score de qualité (jauge)
        // --------------------------------------------------------

        loadQualityScore();

    }


    // ============================================================
    // Dates
    // ============================================================

    private LocalDate getFromDate() {

        return LocalDate.now()
                .minusDays(selectedPeriod.getDays());
    }


    private LocalDate getToDate() {

        return LocalDate.now();
    }


    // ============================================================
    // Concepts / nuage de mots
    // ============================================================
    private List<MultilingualConceptStat> multiLanguageConcepts;
    private void loadTopConcepts(
            LocalDate from,
            LocalDate to) {

        // Top des concepts
        topConcepts =
                statDashboardService.getTopConcepts(
                        from,
                        to,
                        selectedThesaurusId,
                        selectedLanguage,
                        TOP_CONCEPTS_LIMIT
                );

        // Une seule requête pour les langues
        List<ConceptLanguageStat> languageStats =
                statDashboardService.getConceptLanguageStats(
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay(),
                        topConcepts
                );

        // Association avec les ConceptStat
        attachLanguageStats(
                topConcepts,
                languageStats
        );

        // Statistiques de répartition linguistique
        buildLanguageDistributions(topConcepts);

        // Concepts consultés dans plusieurs langues
        multiLanguageConcepts =
                buildMultiLanguageConcepts(topConcepts);

        // Nuage de mots
        wordCloudEntries =
                buildWordCloudEntries(topConcepts);
    }

    private List<MultilingualConceptStat>
    buildMultiLanguageConcepts(
            List<ConceptStat> concepts) {

        return concepts.stream()
                .filter(c ->
                        c.getLanguageStats() != null
                                && c.getLanguageStats().size() > 1
                )
                .map(c -> {

                    String languages =
                            c.getLanguageStats()
                                    .stream()
                                    .map(ConceptLanguageStat::getLanguage)
                                    .map(String::toUpperCase)
                                    .distinct()
                                    .sorted()
                                    .collect(Collectors.joining(" / "));

                    long total =
                            c.getLanguageStats()
                                    .stream()
                                    .mapToLong(
                                            ConceptLanguageStat::getNbVues
                                    )
                                    .sum();

                    return new MultilingualConceptStat(
                            c.getConceptId(),
                            c.getConceptLabel(),
                            c.getThesaurusId(),
                            languages,
                            total
                    );
                })
                .sorted(
                        Comparator.comparingLong(
                                MultilingualConceptStat::getTotalVues
                        ).reversed()
                )
                .toList();
    }

    private void buildLanguageDistributions(
            List<ConceptStat> concepts) {

        for (ConceptStat concept : concepts) {

            List<ConceptLanguageStat> stats =
                    concept.getLanguageStats();

            if (stats == null || stats.isEmpty()) {
                concept.setLanguageDistribution(List.of());
                concept.setLanguageTotal(0);
                continue;
            }

            long total =
                    stats.stream()
                            .mapToLong(ConceptLanguageStat::getNbVues)
                            .sum();

            concept.setLanguageTotal(total);

            List<LanguageDistributionStat> distribution =
                    stats.stream()
                            .map(stat -> {

                                double percentage =
                                        total == 0
                                                ? 0
                                                : (stat.getNbVues() * 100.0) / total;

                                return new LanguageDistributionStat(
                                        stat.getLanguage(),
                                        stat.getNbVues(),
                                        percentage
                                );
                            })
                            .toList();

            concept.setLanguageDistribution(distribution);
        }
    }

    private void attachLanguageStats(
            List<ConceptStat> concepts,
            List<ConceptLanguageStat> languageStats) {

        Map<String, ConceptStat> conceptMap =
                concepts.stream()
                        .collect(Collectors.toMap(
                                c -> c.getThesaurusId() + "|" + c.getConceptId(),
                                Function.identity()
                        ));

        for (ConceptLanguageStat stat : languageStats) {

            String key =
                    stat.getThesaurusId()
                            + "|"
                            + stat.getConceptId();

            ConceptStat concept = conceptMap.get(key);

            if (concept != null) {

                if (concept.getLanguageStats() == null) {
                    concept.setLanguageStats(new ArrayList<>());
                }

                concept.getLanguageStats().add(stat);
            }
        }
    }


    /**
     * Prépare les données du nuage de mots.
     *
     * La taille du texte est proportionnelle au nombre
     * de consultations.
     */
    private List<WordCloudEntry> buildWordCloudEntries(
            List<ConceptStat> concepts) {

        List<WordCloudEntry> entries =
                new ArrayList<>();

        if (concepts == null || concepts.isEmpty()) {
            return entries;
        }


        long max =
                concepts.stream()
                        .mapToLong(
                                ConceptStat::getTotalVues
                        )
                        .max()
                        .orElse(1);


        long min =
                concepts.stream()
                        .mapToLong(
                                ConceptStat::getTotalVues
                        )
                        .min()
                        .orElse(0);


        long range =
                Math.max(
                        max - min,
                        1
                );


        for (ConceptStat concept : concepts) {

            String label =
                    concept.getConceptLabel();


            if (label == null || label.isBlank()) {

                label =
                        concept.getConceptId();
            }


            double ratio =
                    (double)
                            (concept.getTotalVues() - min)
                            / range;


            int fontSize =
                    MIN_WORD_CLOUD_FONT_SIZE
                            + (int) Math.round(
                            ratio *
                                    (
                                            MAX_WORD_CLOUD_FONT_SIZE
                                                    - MIN_WORD_CLOUD_FONT_SIZE
                                    )
                    );


            entries.add(
                    new WordCloudEntry(
                            label,
                            concept.getTotalVues(),
                            fontSize
                    )
            );
        }


        return entries;
    }


    // ============================================================
    // Trafic quotidien
    // ============================================================

    private void loadDailyTrafficChart(
            LocalDate from,
            LocalDate to) {

        List<DailyTrafficStat> data =
                statDashboardService.getDailyTraffic(
                        from,
                        to,
                        selectedThesaurusId
                );


        LineDataset dataSet =
                new LineDataset()
                        .setLabel("Consultations")
                        .setBorderColor(
                                "rgb(75, 130, 180)"
                        )
                        .setBackgroundColor(
                                "rgba(75, 130, 180, 0.2)"
                        )
                        .setFill(
                                new Fill<>(true)
                        );


        LineData lineData =
                new LineData();


        for (DailyTrafficStat stat : data) {

            lineData.addLabels(
                    stat.getDate()
                            .format(CHART_DATE_FORMAT)
            );

            dataSet.addData(
                    stat.getTotalVues()
            );
        }


        lineData.addDataset(dataSet);


        dailyTrafficChartJson =
                new LineChart()
                        .setData(lineData)
                        .toJson();
    }


    // ============================================================
    // Répartition par thésaurus
    // ============================================================

    private void loadThesaurusPieChart(
            LocalDate from,
            LocalDate to) {

        /*
         * La langue est maintenant passée au service.
         *
         * Les statistiques restent indépendantes de la langue.
         * Seul le label affiché change.
         */
        List<ThesaurusStat> data =
                statDashboardService.getTrafficByThesaurus(
                        from,
                        to
                );


        PieDataset dataSet =
                new PieDataset();

        PieData pieData =
                new PieData();


        String[] palette = {
                "rgb(75, 130, 180)",
                "rgb(90, 160, 90)",
                "rgb(220, 150, 60)",
                "rgb(180, 90, 90)",
                "rgb(130, 100, 180)",
                "rgb(90, 170, 170)"
        };


        for (int i = 0; i < data.size(); i++) {

            ThesaurusStat stat =
                    data.get(i);


            String label =
                    stat.getThesaurusLabel();


            if (label == null || label.isBlank()) {

                label =
                        stat.getThesaurusId();
            }


            pieData.addLabels(label);

            dataSet.addData(
                    stat.getTotalVues()
            );

            dataSet.addBackgroundColors(
                    palette[i % palette.length]
            );
        }


        pieData.addDataset(dataSet);


        thesaurusPieChartJson =
                new PieChart()
                        .setData(pieData)
                        .toJson();
    }


    // ============================================================
    // API
    // ============================================================

    private void loadApiUsageChart(
            LocalDateTime from,
            LocalDateTime to) {

        List<ApiUsageStat> data =
                statDashboardService.getApiUsage(
                        from,
                        to
                );


        BarDataset dataSet =
                new BarDataset()
                        .setLabel("Appels API")
                        .setBackgroundColor(
                                "rgb(90, 160, 90)"
                        );


        BarData barData =
                new BarData();


        for (ApiUsageStat stat : data) {

            barData.addLabels(
                    stat.getHttpMethod()
                            + " "
                            + stat.getUrl()
            );

            dataSet.addData(
                    stat.getNbAppels()
            );
        }


        barData.addDataset(dataSet);


        apiUsageChartJson =
                new BarChart()
                        .setData(barData)
                        .toJson();
    }


    // ============================================================
    // Recherches
    // ============================================================

    private void loadSearchStats(
            LocalDateTime from,
            LocalDateTime to) {

        topFailedSearches =
                statDashboardService.getTopFailedSearches(
                        from,
                        to,
                        selectedThesaurusId,
                        TOP_SEARCHES_LIMIT
                );


        topSynonymUsage =
                statDashboardService.getTopSynonymUsage(
                        from,
                        to,
                        selectedThesaurusId,
                        TOP_SEARCHES_LIMIT
                );


        topGlobalSearches =
                statDashboardService.getTopGlobalSearches(
                        from,
                        to,
                        selectedThesaurusId,
                        TOP_SEARCHES_LIMIT
                );
    }
}