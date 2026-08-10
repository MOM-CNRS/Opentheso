package fr.cnrs.opentheso.bean.toolbox.statistique;

import fr.cnrs.opentheso.models.statistiques.ConceptStatisticData;
import fr.cnrs.opentheso.models.statistiques.GenericStatistiqueData;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.candidats.DomaineDto;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.services.statistiques.StatistiqueService;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.services.statistiques.StatistiquesRapportCSV;
import fr.cnrs.opentheso.v2.shared.chart.ChartJsonSupport;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;



@Getter
@Setter
@Slf4j
@SessionScoped
@RequiredArgsConstructor
@Named(value = "statistiqueBean")
public class StatistiqueBean implements Serializable {
    
    private final SelectedTheso selectedTheso;
    private final StatistiqueService statistiqueService;
    private final ThesaurusService thesaurusService;
    private final UserService userService;
    private final ConceptService conceptService;

    private boolean genericTypeVisible, conceptTypeVisible;
    private String selectedStatistiqueTypeCode, selectedCollection, norResult, selectedLanguage;
    private int nbrConceptByThesaurus, nbrCandidateByThesaurus, nbrDeprecatedByThesaurus;
    private Date dateDebut, dateFin, dernierModification;
    private ConceptStatisticData conceptStatistiqueSelected;

    private List<GenericStatistiqueData> genericStatistiques;
    private List<ConceptStatisticData> conceptStatistiques;
    private List<NodeLangTheso> languagesOfThesaurus;
    private List<DomaineDto> groupList;

    private List<String> colors = new ArrayList<>(List.of("rgb(255, 99, 132)","rgb(54, 162, 235)",
            "rgb(75, 192, 192)","rgb(158, 14, 64)",
            "rgb(136, 66, 29)", "rgb(240, 195, 0)", "rgb(63, 34, 4)", "rgb(29, 96, 198)",
            "rgb(121, 248, 248)", "rgb(0, 204, 203)", "rgb(23, 101, 125)", "rgb(102, 0, 255)",
            "rgb(0, 255, 0)", "rgb(135, 233, 144)", "rgb(9, 106, 9)", "rgb(112, 141, 35)",
            "rgb(255, 205, 86)"));

    
    public void init() {

        log.debug("Initialisation de l'interface statistiques");
        genericTypeVisible = false;
        conceptTypeVisible = false;

        genericStatistiques = new ArrayList<>();
        conceptStatistiques = new ArrayList<>();

        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showErrorMessage("Vous devez choisir un Thésaurus avant !");
            return;
        }

        selectedLanguage = selectedTheso.getCurrentLang();
        initChamps();
    }

    public void clearFilter(){
        dateDebut = null;
        dateFin = null;
        selectedCollection = "";
        conceptStatistiques = new ArrayList<>();
        genericStatistiques = new ArrayList<>();
    }

    public String createChartModel(int model) {

        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();

        int pos = 0;
        for (GenericStatistiqueData genericStatistiqueData : genericStatistiques) {
            switch(model) {
                case 1:
                    values.add(genericStatistiqueData.getConceptsNbr());
                    break;
                case 2:
                    values.add(genericStatistiqueData.getSynonymesNbr());
                    break;
                case 3:
                    values.add(genericStatistiqueData.getTermesNonTraduitsNbr());
                    break;
                case 4:
                    values.add(genericStatistiqueData.getNotesNbr());
                    break;
            }
            labels.add(genericStatistiqueData.getCollection());
            bgColors.add(colors.get(pos));
            pos ++;
            if (pos == colors.size()) pos = 0;
        }

        return ChartJsonSupport.doughnut(values, labels, bgColors);
    }

    public void onSelectStatType() {
        
        genericStatistiques = new ArrayList<>();
        conceptStatistiques = new ArrayList<>();

        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            MessageUtils.showWarnMessage("Vous devez choisir un thésaurus avant !");
            return;
        }

        if (CollectionUtils.isEmpty(languagesOfThesaurus)) {
            initChamps();
        }

        genericTypeVisible = false;
        conceptTypeVisible = false;
    }

    private void initChamps() {

        log.debug("Initialisation des champs de l'interface statistiques");

        log.debug("Recupération de la liste des langues du thésaurus {} ({})", selectedTheso.getThesoName(), selectedTheso.getCurrentIdTheso());
        languagesOfThesaurus = thesaurusService.getAllUsedLanguagesOfThesaurusNode(selectedTheso.getSelectedIdTheso(), selectedTheso.getCurrentLang());

        log.debug("Recherche de la liste des groupes présent dans le thésaurus {} ({})", selectedTheso.getThesoName(), selectedTheso.getCurrentIdTheso());
        groupList = statistiqueService.getListGroupes(selectedTheso.getSelectedIdTheso(), selectedTheso.getCurrentLang());
    }

    public List<String> searchDomaineName(String enteredValue) {

        return "%".equals(enteredValue)
                ? groupList.stream().map(DomaineDto::getName).collect(Collectors.toList())
                : groupList.stream().filter((s) -> (s.getName() != null && s.getName().toLowerCase().startsWith(enteredValue.toLowerCase())))
                    .map(DomaineDto::getName).toList();
    }

    private String searchGroupIdFromLabel(String label) {

        if (CollectionUtils.isNotEmpty(groupList)) {
            var groupLabel = groupList.stream().filter(element -> element.getName().equals(label)).findFirst();
            return groupLabel.isPresent() ? groupLabel.get().getId() : "";
        }
        return "";
    }

    public String formatLanguage(String langLabel) {
        return langLabel.substring(0, 1).toUpperCase() + langLabel.substring(1);
    }

    public void onSelectLanguageType() {

        log.debug("Début de l'analyse des données des statistique");

        onSelectStatType();
        clearFilter();

        if ("0".equals(selectedStatistiqueTypeCode)) {

            genericStatistiques = statistiqueService.searchAllCollectionsByThesaurus(selectedTheso.getCurrentIdTheso(), selectedLanguage);

            nbrConceptByThesaurus = statistiqueService.countValidConceptsByThesaurus(selectedTheso.getCurrentIdTheso());

            var conceptsList = statistiqueService.findAllByIdThesaurusAndStatus(selectedTheso.getCurrentIdTheso(), "CA");
            nbrCandidateByThesaurus = CollectionUtils.isNotEmpty(conceptsList) ? conceptsList.size() : 0;

            var deprecatedConceptsList = statistiqueService.findAllByIdThesaurusAndStatus(selectedTheso.getCurrentIdTheso(), "DEP");
            nbrDeprecatedByThesaurus = CollectionUtils.isNotEmpty(deprecatedConceptsList) ? deprecatedConceptsList.size() : 0;

            dernierModification = conceptService.getLastModification(selectedTheso.getCurrentIdTheso());

            genericTypeVisible = true;
            conceptTypeVisible = false;
        } else {
            genericTypeVisible = false;
            conceptTypeVisible = true;
        }

        PrimeFaces.current().ajax().update("messageIndex");
        PrimeFaces.current().ajax().update("containerIndex");
    }
    
    public boolean isExportVisible() {
        return !CollectionUtils.isEmpty(genericStatistiques) || !CollectionUtils.isEmpty(conceptStatistiques);
    }

    public void getStatisticByConcept() {
        conceptStatistiques = statistiqueService.searchAllConceptsByThesaurus(selectedTheso.getCurrentIdTheso(),
                selectedLanguage, dateDebut, dateFin, searchGroupIdFromLabel(selectedCollection), norResult);
    }

    public StreamedContent exportStatistiques() {

        log.debug("Début de l'export des statistiques du thésaurus {}", selectedTheso.getThesoName());
        var statistiquesRapportCSV = new StatistiquesRapportCSV();
        if (genericTypeVisible) {
            log.debug("Statistiques générique sélectionné");
            statistiquesRapportCSV.createGenericStatistiquesRapport(genericStatistiques);
        } else {
            log.debug("Statistiques concepts sélectionné");
            statistiquesRapportCSV.createConceptsStatistiquesRapport(conceptStatistiques);
        }

        log.debug("Recherche des données terminée, début de la génération du fichier");
        return DefaultStreamedContent.builder()
                .contentType("text/csv")
                .name(selectedTheso.getThesoName() + ".csv")
                .stream(() -> new ByteArrayInputStream(statistiquesRapportCSV.getOutput().toByteArray()))
                .build();
    }
    
    public void setConceptSelected(ConceptStatisticData conceptStatistiqueSelected) {
        this.conceptStatistiqueSelected = conceptStatistiqueSelected;
    }
}
