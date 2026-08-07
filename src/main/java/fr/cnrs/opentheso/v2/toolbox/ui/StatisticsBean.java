package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusStatisticsService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.donut.DonutChartDataSet;
import org.primefaces.model.charts.donut.DonutChartModel;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ViewScoped
@Named("v2StatisticsBean")
public class StatisticsBean implements Serializable {

    private final UserSession userSession;
    private final ToolboxAccessPolicy toolboxAccessPolicy;
    private final ThesaurusContext thesaurusContext;
    private final ThesaurusStatisticsService thesaurusStatisticsService;

    private boolean genericTypeVisible;
    private boolean conceptTypeVisible;
    private String selectedStatistiqueTypeCode;
    private String selectedCollection;
    private String resultLimit;
    private String selectedLanguage;

    private int conceptCount;
    private int candidateCount;
    private int deprecatedCount;
    private Date startDate;
    private Date endDate;
    private Date lastModification;

    private ConceptStatisticData selectedConcept;
    private List<GenericStatistiqueData> collectionStatistics = Collections.emptyList();
    private List<ConceptStatisticData> conceptStatistics = Collections.emptyList();
    private List<fr.cnrs.opentheso.models.thesaurus.NodeLangTheso> languages = Collections.emptyList();
    private List<DomaineDto> collections = Collections.emptyList();

    private DonutChartModel conceptsChartModel = emptyChartModel();
    private DonutChartModel synonymsChartModel = emptyChartModel();
    private DonutChartModel untranslatedChartModel = emptyChartModel();
    private DonutChartModel notesChartModel = emptyChartModel();

    private final List<String> chartColors = new ArrayList<>(List.of(
            "rgb(255, 99, 132)", "rgb(54, 162, 235)", "rgb(75, 192, 192)", "rgb(158, 14, 64)",
            "rgb(136, 66, 29)", "rgb(240, 195, 0)", "rgb(63, 34, 4)", "rgb(29, 96, 198)",
            "rgb(121, 248, 248)", "rgb(0, 204, 203)", "rgb(23, 101, 125)", "rgb(102, 0, 255)",
            "rgb(0, 255, 0)", "rgb(135, 233, 144)", "rgb(9, 106, 9)", "rgb(112, 141, 35)",
            "rgb(255, 205, 86)"
    ));

    public StatisticsBean(
            UserSession userSession,
            ToolboxAccessPolicy toolboxAccessPolicy,
            ThesaurusContext thesaurusContext,
            ThesaurusStatisticsService thesaurusStatisticsService
    ) {
        this.userSession = userSession;
        this.toolboxAccessPolicy = toolboxAccessPolicy;
        this.thesaurusContext = thesaurusContext;
        this.thesaurusStatisticsService = thesaurusStatisticsService;
    }

    public boolean isScreenAvailable() {
        return toolboxAccessPolicy.canViewStatistics(userSession)
                && toolboxAccessPolicy.hasSelectedThesaurus(getActiveThesaurusId());
    }

    public String getThesaurusTitle() {
        return thesaurusContext.getCurrentThesaurusTitle();
    }

    public String getThesaurusId() {
        return thesaurusContext.getCurrentThesaurusId();
    }

    public void load() {
        thesaurusContext.syncFromViewParams();
        resetState();
        if (!isScreenAvailable()) {
            MessageUtils.showErrorMessage("Vous devez choisir un Thésaurus avant !");
            return;
        }
        selectedLanguage = resolveInterfaceLanguage();
        selectedStatistiqueTypeCode = "0";
        resultLimit = "100";
        loadReferenceData(false);
    }

    public void initOnModeChange() {
        genericTypeVisible = false;
        conceptTypeVisible = false;
        clearStatisticsResults();

        if (!isScreenAvailable()) {
            MessageUtils.showErrorMessage("Vous devez choisir un Thésaurus avant !");
            return;
        }

        if (StringUtils.isBlank(selectedLanguage)) {
            selectedLanguage = resolveInterfaceLanguage();
        }
        // Collections are only needed for "by concept" filters.
        loadReferenceData("1".equals(selectedStatistiqueTypeCode));
    }

    public void onSelectStatType() {
        clearStatisticsResults();
        genericTypeVisible = false;
        conceptTypeVisible = false;
        if (!isScreenAvailable()) {
            MessageUtils.showWarnMessage("Vous devez choisir un thésaurus avant !");
            return;
        }
        if (CollectionUtils.isEmpty(languages)) {
            loadReferenceData(false);
        }
    }

    public void clearFilter() {
        startDate = null;
        endDate = null;
        selectedCollection = "";
        resultLimit = "100";
        conceptStatistics = new ArrayList<>();
        collectionStatistics = new ArrayList<>();
        clearCharts();
    }

    public void applyLanguageSelection() {
        if (!isScreenAvailable()) {
            MessageUtils.showWarnMessage("Vous devez choisir un thésaurus avant !");
            return;
        }
        if (StringUtils.isBlank(selectedStatistiqueTypeCode)) {
            MessageUtils.showWarnMessage("Veuillez choisir un mode de statistique");
            return;
        }
        genericTypeVisible = false;
        conceptTypeVisible = false;
        clearFilter();
        if ("0".equals(selectedStatistiqueTypeCode)) {
            loadGeneralStatistics();
            genericTypeVisible = true;
        } else {
            if (CollectionUtils.isEmpty(collections)) {
                loadCollectionsOnly();
            }
            conceptTypeVisible = true;
        }
        PrimeFaces.current().ajax().update("containerIndex messageIndex");
    }

    public void loadConceptStatistics() {
        if (!isScreenAvailable()) {
            return;
        }
        conceptStatistics = thesaurusStatisticsService.loadConceptStatistics(
                getActiveThesaurusId(),
                selectedLanguage,
                startDate,
                endDate,
                resolveCollectionId(selectedCollection),
                resultLimit
        );
    }

    public List<String> searchCollectionName(String query) {
        if (CollectionUtils.isEmpty(collections)) {
            return List.of();
        }
        if ("%".equals(query)) {
            return collections.stream().map(DomaineDto::getName).collect(Collectors.toList());
        }
        return collections.stream()
                .filter(item -> item.getName() != null && item.getName().toLowerCase().startsWith(query.toLowerCase()))
                .map(DomaineDto::getName)
                .toList();
    }

    public String formatLanguage(String label) {
        if (StringUtils.isBlank(label)) {
            return "";
        }
        return label.substring(0, 1).toUpperCase() + label.substring(1);
    }

    public boolean isExportVisible() {
        return CollectionUtils.isNotEmpty(collectionStatistics) || CollectionUtils.isNotEmpty(conceptStatistics);
    }

    public StreamedContent exportStatistics() {
        byte[] content = genericTypeVisible
                ? thesaurusStatisticsService.exportGenericReport(collectionStatistics)
                : thesaurusStatisticsService.exportConceptReport(conceptStatistics);
        String fileName = StringUtils.defaultIfBlank(getThesaurusTitle(), "statistics") + ".csv";
        return DefaultStreamedContent.builder()
                .contentType("text/csv")
                .name(fileName)
                .stream(() -> new ByteArrayInputStream(content))
                .build();
    }

    public void selectConcept(ConceptStatisticData concept) {
        this.selectedConcept = concept;
    }

    public String getSelectedConceptLabel() {
        return selectedConcept != null ? selectedConcept.getLabel() : "";
    }

    private void loadGeneralStatistics() {
        String thesaurusId = getActiveThesaurusId();
        collectionStatistics = thesaurusStatisticsService.loadCollectionStatistics(
                thesaurusId,
                selectedLanguage
        );
        var summary = thesaurusStatisticsService.loadSummary(thesaurusId);
        conceptCount = summary.counts().conceptCount();
        candidateCount = summary.counts().candidateCount();
        deprecatedCount = summary.counts().deprecatedCount();
        lastModification = summary.lastModification();
        refreshCharts();
    }

    private void loadReferenceData(boolean includeCollections) {
        String thesaurusId = getActiveThesaurusId();
        String interfaceLanguage = resolveInterfaceLanguage();
        if (CollectionUtils.isEmpty(languages)) {
            languages = thesaurusStatisticsService.loadLanguages(thesaurusId, interfaceLanguage);
        }
        if (includeCollections) {
            loadCollectionsOnly();
        }
    }

    private void loadCollectionsOnly() {
        collections = thesaurusStatisticsService.loadCollections(
                getActiveThesaurusId(),
                resolveInterfaceLanguage()
        );
    }

    private String getActiveThesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    private String resolveInterfaceLanguage() {
        return thesaurusContext.resolveWorkLanguage();
    }

    private String resolveCollectionId(String label) {
        if (CollectionUtils.isEmpty(collections) || StringUtils.isBlank(label)) {
            return "";
        }
        return collections.stream()
                .filter(item -> label.equals(item.getName()))
                .map(DomaineDto::getId)
                .findFirst()
                .orElse("");
    }

    private void resetState() {
        genericTypeVisible = false;
        conceptTypeVisible = false;
        clearStatisticsResults();
        languages = Collections.emptyList();
        collections = Collections.emptyList();
    }

    private void clearStatisticsResults() {
        collectionStatistics = new ArrayList<>();
        conceptStatistics = new ArrayList<>();
        clearCharts();
    }

    private void clearCharts() {
        conceptsChartModel = emptyChartModel();
        synonymsChartModel = emptyChartModel();
        untranslatedChartModel = emptyChartModel();
        notesChartModel = emptyChartModel();
    }

    private void refreshCharts() {
        conceptsChartModel = buildChartModel(1);
        synonymsChartModel = buildChartModel(2);
        untranslatedChartModel = buildChartModel(3);
        notesChartModel = buildChartModel(4);
    }

    private DonutChartModel buildChartModel(int model) {
        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();
        int pos = 0;
        for (GenericStatistiqueData row : collectionStatistics) {
            switch (model) {
                case 1 -> values.add(row.getConceptsNbr());
                case 2 -> values.add(row.getSynonymesNbr());
                case 3 -> values.add(row.getTermesNonTraduitsNbr());
                case 4 -> values.add(row.getNotesNbr());
                default -> {
                }
            }
            labels.add(row.getCollection());
            bgColors.add(chartColors.get(pos));
            pos = (pos + 1) % chartColors.size();
        }
        return toDonutModel(values, labels, bgColors);
    }

    private static DonutChartModel emptyChartModel() {
        return toDonutModel(List.of(), List.of(), List.of());
    }

    private static DonutChartModel toDonutModel(List<Number> values, List<String> labels, List<String> bgColors) {
        var dataSet = new DonutChartDataSet();
        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        var data = new ChartData();
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        var donutModel = new DonutChartModel();
        donutModel.setData(data);
        return donutModel;
    }
}
