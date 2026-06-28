package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
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
    private final CurrentUser currentUser;
    private final ThesaurusContext thesaurusContext;
    private final SelectedTheso selectedTheso;
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

    private final List<String> chartColors = new ArrayList<>(List.of(
            "rgb(255, 99, 132)", "rgb(54, 162, 235)", "rgb(75, 192, 192)", "rgb(158, 14, 64)",
            "rgb(136, 66, 29)", "rgb(240, 195, 0)", "rgb(63, 34, 4)", "rgb(29, 96, 198)",
            "rgb(121, 248, 248)", "rgb(0, 204, 203)", "rgb(23, 101, 125)", "rgb(102, 0, 255)",
            "rgb(0, 255, 0)", "rgb(135, 233, 144)", "rgb(9, 106, 9)", "rgb(112, 141, 35)",
            "rgb(255, 205, 86)"
    ));

    public StatisticsBean(
            UserSession userSession,
            CurrentUser currentUser,
            ThesaurusContext thesaurusContext,
            SelectedTheso selectedTheso,
            ThesaurusStatisticsService thesaurusStatisticsService
    ) {
        this.userSession = userSession;
        this.currentUser = currentUser;
        this.thesaurusContext = thesaurusContext;
        this.selectedTheso = selectedTheso;
        this.thesaurusStatisticsService = thesaurusStatisticsService;
    }

    public boolean isScreenAvailable() {
        return ToolboxAccessPolicy.canViewStatistics(userSession, currentUser)
                && ToolboxAccessPolicy.hasSelectedThesaurus(getActiveThesaurusId());
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
        loadReferenceData();
    }

    public void initOnModeChange() {
        genericTypeVisible = false;
        conceptTypeVisible = false;
        collectionStatistics = new ArrayList<>();
        conceptStatistics = new ArrayList<>();

        if (!isScreenAvailable()) {
            MessageUtils.showErrorMessage("Vous devez choisir un Thésaurus avant !");
            return;
        }

        selectedLanguage = resolveInterfaceLanguage();
        loadReferenceData();
    }

    public void onSelectStatType() {
        collectionStatistics = new ArrayList<>();
        conceptStatistics = new ArrayList<>();
        genericTypeVisible = false;
        conceptTypeVisible = false;
        if (!isScreenAvailable()) {
            MessageUtils.showWarnMessage("Vous devez choisir un thésaurus avant !");
            return;
        }
        if (CollectionUtils.isEmpty(languages)) {
            loadReferenceData();
        }
    }

    public void clearFilter() {
        startDate = null;
        endDate = null;
        selectedCollection = "";
        conceptStatistics = new ArrayList<>();
        collectionStatistics = new ArrayList<>();
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

    public DonutChartModel createChartModel(int model) {
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

    public StreamedContent exportStatistics() {
        byte[] content = genericTypeVisible
                ? thesaurusStatisticsService.exportGenericReport(collectionStatistics)
                : thesaurusStatisticsService.exportConceptReport(conceptStatistics);
        return DefaultStreamedContent.builder()
                .contentType("text/csv")
                .name(getThesaurusTitle() + ".csv")
                .stream(() -> thesaurusStatisticsService.toStream(content))
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
    }

    private void loadReferenceData() {
        String thesaurusId = getActiveThesaurusId();
        String interfaceLanguage = resolveInterfaceLanguage();
        languages = thesaurusStatisticsService.loadLanguages(thesaurusId, interfaceLanguage);
        collections = thesaurusStatisticsService.loadCollections(thesaurusId, interfaceLanguage);
    }

    private String getActiveThesaurusId() {
        if (StringUtils.isNotBlank(thesaurusContext.getCurrentThesaurusId())) {
            return thesaurusContext.getCurrentThesaurusId();
        }
        return selectedTheso.getCurrentIdTheso();
    }

    private String resolveInterfaceLanguage() {
        return StringUtils.isNotBlank(selectedTheso.getCurrentLang())
                ? selectedTheso.getCurrentLang()
                : "fr";
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
        collectionStatistics = new ArrayList<>();
        conceptStatistics = new ArrayList<>();
        languages = Collections.emptyList();
        collections = Collections.emptyList();
    }
}
