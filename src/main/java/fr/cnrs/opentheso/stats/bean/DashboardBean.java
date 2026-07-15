package fr.cnrs.opentheso.stats.bean;

import fr.cnrs.opentheso.stats.dto.ApiUsageStat;
import fr.cnrs.opentheso.stats.dto.ConceptStat;
import fr.cnrs.opentheso.stats.dto.DailyTrafficStat;
import fr.cnrs.opentheso.stats.dto.ThesaurusStat;
import fr.cnrs.opentheso.stats.services.StatDashboardService;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.charts.PieChart;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.data.PieData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.dataset.PieDataset;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import lombok.Data;
import software.xdev.chartjs.model.options.elements.Fill;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Bean JSF alimentant la page de tableau de bord statistique (dashboard.xhtml).
 * Recalcule tous les indicateurs à chaque changement de période sélectionnée.
 *
 * Utilise chartjs-java-model (software.xdev), intégration officielle
 * recommandée par PrimeFaces depuis la version 14, en remplacement des
 * anciennes classes org.primefaces.model.charts.*.
 *
 * Note : les couleurs sont passées sous forme de chaînes CSS classiques
 * ("rgb(...)", "rgba(...)", "#rrggbb"...), la librairie n'imposant plus de
 * classe Color dédiée dans ses versions récentes.
 *
 * Chaque modèle est sérialisé en JSON brut via toJson(), consommé côté page
 * par l'attribut "value" du composant p:chart.
 */
@Data
@Named
@ViewScoped
public class DashboardBean implements Serializable {

    @Inject
    private StatDashboardService statDashboardService;

    // Injectez ici votre service existant de résolution de concepts,
    // utilisé pour retrouver le libellé ACTUEL d'un concept à partir de son id
    // (ex: @Inject private ConceptService conceptService;)

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

    private List<ConceptStat> topConcepts;
    private String wordCloudDataAsJson;

    // Modèles de graphiques : JSON brut produit par chartjs-java-model,
    // à consommer côté page via <p:chart value="#{dashboardBean.xxx}"/>
    private String dailyTrafficChartJson;
    private String thesaurusPieChartJson;
    private String apiUsageChartJson;

    @PostConstruct
    public void init() {
        refreshDashboard();
    }

    /** Appelé en ajax listener depuis le sélecteur de période dans la page. */
    public void onPeriodChange() {
        refreshDashboard();
    }

    private void refreshDashboard() {
        LocalDate from = LocalDate.now().minusDays(selectedPeriod.getDays());
        LocalDate to = LocalDate.now();

        loadTopConcepts(from, to);
        loadDailyTrafficChart(from, to);
        loadThesaurusPieChart(from, to);
        loadApiUsageChart(from.atStartOfDay(), to.atStartOfDay().plusDays(1));
    }

    private void loadTopConcepts(LocalDate from, LocalDate to) {
        topConcepts = statDashboardService.getTopConcepts(from, to, 50);

        // TODO : remplacer ceci par un appel à votre service de concepts existant
        // pour récupérer le libellé ACTUEL de chaque concept (voir ConceptStat).
        for (ConceptStat concept : topConcepts) {
            if (concept.getLabel() == null) {
                concept.setLabel(concept.getConceptId()); // solution de repli temporaire
            }
        }

        wordCloudDataAsJson = buildWordCloudJson(topConcepts);
    }

    private String buildWordCloudJson(List<ConceptStat> concepts) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < concepts.size(); i++) {
            ConceptStat c = concepts.get(i);
            if (i > 0) {
                json.append(",");
            }
            String safeLabel = c.getLabel().replace("\"", "\\\"");
            json.append("[\"").append(safeLabel).append("\",").append(c.getTotalVues()).append("]");
        }
        json.append("]");
        return json.toString();
    }

    private void loadDailyTrafficChart(LocalDate from, LocalDate to) {
        List<DailyTrafficStat> data = statDashboardService.getDailyTraffic(from, to);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        LineDataset dataSet = new LineDataset()
                .setLabel("Consultations")
                .setBorderColor("rgb(75, 130, 180)")
                .setBackgroundColor("rgba(75, 130, 180, 0.2)")
                .setFill(new Fill<>(true));

        LineData lineData = new LineData();
        for (DailyTrafficStat stat : data) {
            lineData.addLabels(stat.getDate().format(formatter));
            dataSet.addData(stat.getTotalVues());
        }
        lineData.addDataset(dataSet);

        this.dailyTrafficChartJson = new LineChart()
                .setData(lineData)
                .toJson();
    }

    private void loadThesaurusPieChart(LocalDate from, LocalDate to) {
        List<ThesaurusStat> data = statDashboardService.getTrafficByThesaurus(from, to);

        PieDataset dataSet = new PieDataset();
        PieData pieData = new PieData();

        String[] palette = {
                "rgb(75, 130, 180)", "rgb(90, 160, 90)", "rgb(220, 150, 60)",
                "rgb(180, 90, 90)", "rgb(130, 100, 180)", "rgb(90, 170, 170)"
        };
        int i = 0;
        for (ThesaurusStat stat : data) {
            pieData.addLabels(stat.getThesaurusLabel());
            dataSet.addData(stat.getTotalVues());
            dataSet.addBackgroundColors(palette[i % palette.length]);
            i++;
        }
        pieData.addDataset(dataSet);

        this.thesaurusPieChartJson = new PieChart()
                .setData(pieData)
                .toJson();
    }

    private void loadApiUsageChart(LocalDateTime from, LocalDateTime to) {
        List<ApiUsageStat> data = statDashboardService.getApiUsage(from, to, 15);

        BarDataset dataSet = new BarDataset()
                .setLabel("Appels API")
                .setBackgroundColor("rgb(90, 160, 90)");

        BarData barData = new BarData();
        for (ApiUsageStat stat : data) {
            barData.addLabels(stat.getHttpMethod() + " " + stat.getUrl());
            dataSet.addData(stat.getNbAppels());
        }
        barData.addDataset(dataSet);

        this.apiUsageChartJson = new BarChart()
                .setData(barData)
                .toJson();
    }

    public Period[] getAvailablePeriods() {
        return Period.values();
    }
}