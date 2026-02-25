package fr.cnrs.opentheso.bean.search;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.models.concept.NodeConceptSearch;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import fr.cnrs.opentheso.bean.index.IndexSetting;
import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.bean.leftbody.LeftBodySetting;
import fr.cnrs.opentheso.bean.leftbody.viewgroups.TreeGroups;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.proposition.PropositionBean;
import fr.cnrs.opentheso.bean.rightbody.RightBodySetting;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.services.ThesaurusService;

import java.io.IOException;

import fr.cnrs.opentheso.utils.MessageUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

@Data
@Named(value = "searchBean")
@SessionScoped
@RequiredArgsConstructor
public class SearchBean implements Serializable {

    private final Tree tree;
    private final LanguageBean languageBean;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final RightBodySetting rightBodySetting;
    private final LeftBodySetting leftBodySetting;
    private final ConceptView conceptBean;
    private final IndexSetting indexSetting;
    private final TreeGroups treeGroups;
    private final RoleOnThesaurusBean roleOnThesoBean;
    private final PropositionBean propositionBean;

    private final ThesaurusService thesaurusService;
    private final ConceptService conceptService;
    private final SearchService searchService;


    private NodeSearchMini searchSelected;
    private List<NodeSearchMini> listResultAutoComplete;
    private List<NodeConceptSearch> nodeConceptSearchs;
    private String searchValue;

    // sert à afficher ou non le total de résultat
    private boolean selectedItem;

    // filter search
    private boolean exactMatch;
    private boolean indexMatch;
    private boolean withNote;
    private boolean withId;
    private boolean isSearchInSpecificThesaurus;

    public void clear() {
        if (listResultAutoComplete != null) {
            listResultAutoComplete.clear();
            listResultAutoComplete = null;
        }
        if (nodeConceptSearchs != null) {
            nodeConceptSearchs.clear();
            nodeConceptSearchs = null;
        }
        searchSelected = null;
        searchValue = null;
    }

    public void activateIndexMatch() {
        exactMatch = false;
        withId = false;
        withNote = false;
    }

    public void activateExactMatch() {
        withId = false;
        withNote = false;
        indexMatch = false;
    }

    public void activateWithNote() {
        withId = false;
        exactMatch = false;
        indexMatch = false;
    }

    public void activateWithId() {
        withNote = false;
        exactMatch = false;
        indexMatch = false;
    }

    public void reset() {
        nodeConceptSearchs = new ArrayList<>();
        listResultAutoComplete = new ArrayList<>();
        searchSelected = null;
    }

    public List<NodeSearchMini> completTermFullText(String value) {

        if (selectedTheso == null) {
            return List.of();
        }

        selectedItem = false;
        listResultAutoComplete = new ArrayList<>();
        if(StringUtils.isEmpty(value)) return List.of();
        value = value.trim();

        if (selectedTheso.getCurrentIdTheso() != null && selectedTheso.getSelectedLang() != null) {

            var idLang = "all".equalsIgnoreCase(selectedTheso.getSelectedLang()) ? null : selectedTheso.getSelectedLang();
            var isCollectionPrivate = ObjectUtils.isEmpty(currentUser.getNodeUser());

            if (exactMatch) {
                listResultAutoComplete = searchService.searchExactMatch(value, idLang, selectedTheso.getCurrentIdTheso(), isCollectionPrivate);
            }

            if (indexMatch) {
                listResultAutoComplete = searchService.searchStartWith(value, idLang, selectedTheso.getCurrentIdTheso(), isCollectionPrivate);
            }

            if (withId) {
                listResultAutoComplete = searchService.searchByAllId(value, idLang, selectedTheso.getCurrentIdTheso(), isCollectionPrivate);
            }

            if (withNote) {
                listResultAutoComplete = searchService.searchNotes(value, idLang, selectedTheso.getCurrentIdTheso());
            }

            if (!withId && !withNote && !indexMatch && !exactMatch) {
                listResultAutoComplete = searchService.searchFullTextElastic(value, idLang, selectedTheso.getCurrentIdTheso(), isCollectionPrivate);
            }
        }

        searchValue = value;
        return listResultAutoComplete == null ? Collections.emptyList() : listResultAutoComplete;
    }

    public void onSelect() throws IOException {
        if (searchSelected == null) {
            return;
        }
        String[] values = searchSelected.getIdConcept().split("####");
        String idConcept;
        if (values.length > 1) {
            idConcept = values[0];

            //action group
            if (values[1].equalsIgnoreCase("isGroup")) {
                treeGroups.selectThisGroup(idConcept);
            }
            //action facet
            if (values[1].equalsIgnoreCase("isFacet")) {
                tree.selectThisFacet(idConcept);
            }

        } else {
            idConcept = searchSelected.getIdConcept();

            nodeConceptSearchs = new ArrayList<>();
            nodeConceptSearchs.add(conceptService.getConceptForSearch(idConcept, selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang()));
            setViewsSearch();
            if (nodeConceptSearchs.size() == 1) {
                onSelectConcept(selectedTheso.getCurrentIdTheso(), nodeConceptSearchs.get(0).getIdConcept(), selectedTheso.getCurrentLang());
            }
        }
        selectedItem = true;

        PrimeFaces.current().ajax().update("messageIndex");
        PrimeFaces.current().ajax().update("containerIndex");
    }

    /**
     * recherche de la valeur saisie en respectant les filtres
     */
    public void applySearch() {

        nodeConceptSearchs = new ArrayList<>();

        if (roleOnThesoBean.getSelectedThesaurusForSearch() == null || roleOnThesoBean.getSelectedThesaurusForSearch().isEmpty()) {
            MessageUtils.showErrorMessage("Il faut choisir au moins un thésaurus !");
            return;
        }
        isSearchInSpecificThesaurus = (selectedTheso.getCurrentIdTheso() != null
                && !selectedTheso.getCurrentIdTheso().isEmpty()) || roleOnThesoBean.getSelectedThesaurusForSearch().size() <= 1;

        // cas où la recherche est sur un thésaurus sélectionné, il faut trouver la langue sélectionnée par l'utilisateur, si all, on cherche sur tous les thésaurus 
        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            for (String idThesaurus : roleOnThesoBean.getSelectedThesaurusForSearch()) {
                nodeConceptSearchs.addAll(searchInThesaurus(idThesaurus, searchLangOfTheso(roleOnThesoBean.getListThesaurus(), idThesaurus)));
            }
        } else {
            String idLang = null;
            if (!"all".equalsIgnoreCase(selectedTheso.getSelectedLang())) {
                idLang = selectedTheso.getSelectedLang();
            }
            nodeConceptSearchs.addAll(searchInThesaurus(selectedTheso.getCurrentIdTheso(), idLang));
        }

        if (CollectionUtils.isNotEmpty(nodeConceptSearchs)) {
            if (nodeConceptSearchs.size() == 1) {
                conceptBean.getConcept(nodeConceptSearchs.get(0).getIdTheso(), nodeConceptSearchs.get(0).getIdConcept(),
                        nodeConceptSearchs.get(0).getCurrentLang(), currentUser);
                selectedItem = true;
                setViewsConcept();
            } else {
                setViewsSearch();
                selectedItem = false;
            }

            PrimeFaces.current().executeScript("showResultSearchBar();");
        } else {
            MessageUtils.showWarnMessage("Recherche de '" + searchValue + "' : Aucun resultat trouvée !");
        }

        rightBodySetting.setIndex("0");
        indexSetting.setIsValueSelected(true);

        PrimeFaces.current().ajax().update("resultSearchBar");
    }

    private String searchLangOfTheso(List<RoleOnThesaurusBean.ThesaurusModel> listTheso, String idTheso) {
        for (RoleOnThesaurusBean.ThesaurusModel theso : listTheso) {
            if (theso.getId().equals(idTheso)) {
                return theso.getDefaultLang();
            }
        }
        return selectedTheso.getSelectedLang();
    }

    public String getThesoName(String idTheso, String idLang) {

        return thesaurusService.getTitleOfThesaurus(idTheso, idLang);
    }

    private List<NodeConceptSearch> searchInThesaurus(String idTheso, String idLang) {

        var isPrivate = currentUser.getNodeUser() == null;
        List<NodeConceptSearch> concepts = new ArrayList<>();
        var thesaurusLabel = thesaurusService.getTitleOfThesaurus(idTheso, idLang);
        if(searchValue == null)
            searchValue = "";

        if (withId) {
            var nodeSearchsId = searchService.searchForIds(searchValue, idTheso);
            for (String idConcept : nodeSearchsId) {
                var nodeConceptSearch = conceptService.getConceptForSearch(idConcept, idTheso, idLang);
                if (nodeConceptSearch != null) {
                    nodeConceptSearch.setThesoName(thesaurusLabel);
                }
                concepts.add(0,nodeConceptSearch);
            }            
        }

        if (withNote) {
            var nodeSearchsId = searchService.searchIdConceptFromNotes(searchValue, idLang, idTheso);
            for (String idConcept : nodeSearchsId) {
                var nodeConceptSearch = conceptService.getConceptForSearch(idConcept, idTheso, idLang);
                if (nodeConceptSearch != null) {
                    nodeConceptSearch.setThesoName(thesaurusLabel);
                }
                concepts.add(nodeConceptSearch);
            }
        }

        var isCollectionPrivate = ObjectUtils.isEmpty(currentUser.getNodeUser());

        if (exactMatch) {
            var nodeSearchMinis = searchService.searchExactMatch(searchValue, idLang, idTheso, isCollectionPrivate);
            for (NodeSearchMini nodeSearchMini : nodeSearchMinis) {
                var nodeConceptSearch = conceptService.getConceptForSearch(nodeSearchMini.getIdConcept(), idTheso, idLang);
                if (nodeConceptSearch != null) {
                    nodeConceptSearch.setThesoName(thesaurusLabel);
                }
                concepts.add(nodeConceptSearch);
            }
        }

        if (indexMatch || searchValue.isEmpty()) {
            var nodeSearchMinis = searchService.searchStartWith(searchValue, idLang, idTheso, isCollectionPrivate);
            for (NodeSearchMini nodeSearchMini : nodeSearchMinis) {
                var nodeConceptSearch = conceptService.getConceptForSearch(nodeSearchMini.getIdConcept(), idTheso, idLang);
                if (nodeConceptSearch != null) {
                    nodeConceptSearch.setThesoName(thesaurusLabel);
                }
                concepts.add(nodeConceptSearch);
            }

        }

        if (!withId && !withNote && !exactMatch && !indexMatch) {
            var listIds = searchService.searchFullTextElasticId(searchValue, idLang, idTheso, isPrivate);

            for (String idConcept : listIds) {
                var nodeConceptSearch = conceptService.getConceptForSearch(idConcept, idTheso, idLang);
                if (nodeConceptSearch != null) {
                    nodeConceptSearch.setThesoName(thesaurusLabel);
                }
                concepts.add(nodeConceptSearch);
            }
        }
        return concepts;
    }

    public void afficherResultatRecherche() {
        if (CollectionUtils.isEmpty(nodeConceptSearchs)) {
            MessageUtils.showErrorMessage(languageBean.getMsg("search.doSearchBefore") + " !");
        } else {
            PrimeFaces.current().executeScript("showResultSearchBar();");
        }
    }

    public void setBarSearchStatus() {
        PrimeFaces.current().executeScript("showResultSearchBar();");
    }

    /**
     * permet de retourner la liste des concepts qui ont une poly-hiérarchie
     */
    public void getAllPolyierarchy() throws IOException {
        if (nodeConceptSearchs == null) {
            nodeConceptSearchs = new ArrayList<>();
        } else {
            nodeConceptSearchs.clear();
        }

        List<String> nodeSearchsId = searchService.searchAllPolyHierarchy(selectedTheso.getCurrentIdTheso());

        for (String idConcept : nodeSearchsId) {
            nodeConceptSearchs.add(conceptService.getConceptForSearch(idConcept, selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang()));
        }

        if (!nodeConceptSearchs.isEmpty()) {
            Collections.sort(nodeConceptSearchs);
            onSelectConcept(selectedTheso.getCurrentIdTheso(), nodeConceptSearchs.get(0).getIdConcept(), selectedTheso.getCurrentLang());
        }

        if (nodeConceptSearchs != null && !nodeConceptSearchs.isEmpty()) {
            if (nodeConceptSearchs.size() == 1) {
                selectedItem = true;
                setViewsConcept();
            } else {
                setViewsSearch();
                selectedItem = false;
            }
        } else {
            MessageUtils.showWarnMessage("Recherche Poly-hiéarchie : Aucun résultat trouvée !");
            return;
        }

        rightBodySetting.setIndex("0");
        indexSetting.setIsValueSelected(true);
    }

    /**
     * permet de retourner la liste des concepts qui ont une poly-hiérarchie
     */
    public void getAllDeprecatedConcepts() throws IOException {

        if (nodeConceptSearchs == null) {
            nodeConceptSearchs = new ArrayList<>();
        } else {
            nodeConceptSearchs.clear();
        }

        List<String> nodeSearchsId = searchService.searchAllDeprecatedConcepts(selectedTheso.getCurrentIdTheso());

        for (String idConcept : nodeSearchsId) {
            nodeConceptSearchs.add(conceptService.getConceptForSearch(idConcept,
                    selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang()));
        }

        if (!nodeConceptSearchs.isEmpty()) {
            onSelectConcept(selectedTheso.getCurrentIdTheso(),
                    nodeConceptSearchs.get(0).getIdConcept(), selectedTheso.getCurrentLang());
        }

        if (nodeConceptSearchs != null && !nodeConceptSearchs.isEmpty()) {
            Collections.sort(nodeConceptSearchs);
            if (nodeConceptSearchs.size() == 1) {
                selectedItem = true;
                setViewsConcept();
            } else {
                setViewsSearch();
                selectedItem = false;
            }
        } else {
            MessageUtils.showWarnMessage("Recherche de concepts dépréciés : Pas de résultat !");
            return;
        }

        rightBodySetting.setIndex("0");
        indexSetting.setIsValueSelected(true);
    }

    /**
     * permet de retourner la liste des concepts qui ont plusieurs Groupes
     */
    public void searchConceptWithMultiGroup() throws IOException {
        if (nodeConceptSearchs == null) {
            nodeConceptSearchs = new ArrayList<>();
        } else {
            nodeConceptSearchs.clear();
        }

        List<String> nodeSearchsId = searchService.searchConceptWithMultiGroup(selectedTheso.getCurrentIdTheso());

        for (String idConcept : nodeSearchsId) {
            nodeConceptSearchs.add(conceptService.getConceptForSearch(idConcept, selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang()));
        }
        if (!nodeConceptSearchs.isEmpty()) {
            Collections.sort(nodeConceptSearchs);
            onSelectConcept(selectedTheso.getCurrentIdTheso(), nodeConceptSearchs.get(0).getIdConcept(), selectedTheso.getCurrentLang());
        }
        if (nodeConceptSearchs != null && !nodeConceptSearchs.isEmpty()) {
            if (nodeConceptSearchs.size() == 1) {
                selectedItem = true;
                setViewsConcept();
            } else {
                setViewsSearch();
                selectedItem = false;
            }
        } else {
            MessageUtils.showWarnMessage("Recherche multi-groupes : Aucun résultat trouvée !");
            return;
        }

        rightBodySetting.setIndex("0");
        indexSetting.setIsValueSelected(true);
    }

    /**
     * permet de retourner la liste des concepts qui ont plusieurs Groupes
     */
    public void searchConceptWithoutGroup() throws IOException {
        if (nodeConceptSearchs == null) {
            nodeConceptSearchs = new ArrayList<>();
        } else {
            nodeConceptSearchs.clear();
        }

        List<String> nodeSearchsId = searchService.searchConceptWithoutGroup(selectedTheso.getCurrentIdTheso());

        for (String idConcept : nodeSearchsId) {
            nodeConceptSearchs.add(conceptService.getConceptForSearch(idConcept, selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang()));
        }
        if (!nodeConceptSearchs.isEmpty()) {
            Collections.sort(nodeConceptSearchs);
            onSelectConcept(selectedTheso.getCurrentIdTheso(), nodeConceptSearchs.get(0).getIdConcept(), selectedTheso.getCurrentLang());
        }
        if (nodeConceptSearchs != null && !nodeConceptSearchs.isEmpty()) {
            if (nodeConceptSearchs.size() == 1) {
                selectedItem = true;
                setViewsConcept();
            } else {
                setViewsSearch();
                selectedItem = false;
            }
        } else {
            MessageUtils.showWarnMessage("Recherche sans-groupes : Aucun résultat trouvée !");
            return;
        }

        rightBodySetting.setIndex("0");
        indexSetting.setIsValueSelected(true);
    }

    /**
     * permet de retourner la liste des concepts qui sont en doublons
     */
    public void searchConceptDuplicated() throws IOException {

        nodeConceptSearchs = new ArrayList<>();
        var nodeSearchLabels = searchService.searchConceptDuplicated( selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());

        for (var label : nodeSearchLabels) {
            var element = conceptService.getConceptForSearchFromLabel(label, selectedTheso.getCurrentIdTheso(),
                    selectedTheso.getCurrentLang());
            if (element != null) {
                nodeConceptSearchs.add(element);
            }
        }
        if (CollectionUtils.isNotEmpty(nodeConceptSearchs)) {
            Collections.sort(nodeConceptSearchs);
            onSelectConcept(selectedTheso.getCurrentIdTheso(), nodeConceptSearchs.get(0).getIdConcept(), selectedTheso.getCurrentLang());

            if (nodeConceptSearchs.size() == 1) {
                selectedItem = true;
                setViewsConcept();
            } else {
                setViewsSearch();
                selectedItem = false;
            }
        } else {
            MessageUtils.showWarnMessage("Recherche doublons : Aucun résultat trouvée !");
            return;
        }

        rightBodySetting.setIndex("0");
        indexSetting.setIsValueSelected(true);
    }

    /**
     * permet de retourner la liste des concepts qui sont une relation RT et NT
     * ou BT ce qui est interdit
     */
    public void searchConceptWithRTandBT() throws IOException {
        if (nodeConceptSearchs == null) {
            nodeConceptSearchs = new ArrayList<>();
        } else {
            nodeConceptSearchs.clear();
        }

        var allIdConcepts = conceptService.getAllIdConceptOfThesaurus(selectedTheso.getCurrentIdTheso());

        ArrayList<String> nodeSearchsId = new ArrayList<>();
        for (String idConcept : allIdConcepts) {
            if (searchService.isConceptHaveRTandBT(idConcept, selectedTheso.getCurrentIdTheso())) {
                nodeSearchsId.add(idConcept);
            }
        }
        for (String idConcept : nodeSearchsId) {
            nodeConceptSearchs.add(conceptService.getConceptForSearch(idConcept, selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang()));
        }
        if (!nodeConceptSearchs.isEmpty()) {
            Collections.sort(nodeConceptSearchs);
            onSelectConcept(selectedTheso.getCurrentIdTheso(), nodeConceptSearchs.get(0).getIdConcept(), selectedTheso.getCurrentLang());
        }
        if (nodeConceptSearchs != null && !nodeConceptSearchs.isEmpty()) {
            if (nodeConceptSearchs.size() == 1) {
                selectedItem = true;
                setViewsConcept();
            } else {
                setViewsSearch();
                selectedItem = false;
            }
        } else {
            MessageUtils.showWarnMessage("Recherche relations interdites : Aucun résultat trouvée !");
            return;
        }

        rightBodySetting.setIndex("0");
        indexSetting.setIsValueSelected(true);
    }

    private void setViewsSearch() {
        rightBodySetting.setShowResultSearchToOn();
        leftBodySetting.setIndex("0"); // vue Filtre de recherche
        rightBodySetting.setIndex("0");
    }

    private void setViewsConcept() {
        rightBodySetting.setShowResultSearchToOn();
        leftBodySetting.setIndex("0"); // vue Filtre de recherche
        rightBodySetting.setIndex("0");
    }

    public void onSelectConcept(String idThesaurus, String idConcept, String idLang) throws IOException {

        propositionBean.setNewProposition(false);
        roleOnThesoBean.initNodePref(idThesaurus);
        selectedTheso.setSelectedIdTheso(idThesaurus);
        selectedTheso.setSelectedLang(idLang);
        selectedTheso.setSelectedThesaurusForSearch();

        roleOnThesoBean.setSelectedThesaurusForSearch(roleOnThesoBean.getSelectedThesaurusForSearch().stream()
                .filter(theso -> theso.contains(idThesaurus))
                .collect(Collectors.toList()));
        conceptBean.getConcept(idThesaurus, idConcept, idLang, currentUser);
        rightBodySetting.setIndex("0");
        
        PrimeFaces.current().ajax().update("containerIndex:contentConcept");
        PrimeFaces.current().ajax().update("containerIndex:thesoSelect");
    }

    public NodeSearchMini getSearchSelected() {
        return searchSelected;
    }

    public void setSearchSelected(NodeSearchMini searchSelected) {
        this.searchSelected = searchSelected;
    }

    public List<NodeConceptSearch> getNodeConceptSearchs() {
        return nodeConceptSearchs;
    }

    public void setNodeConceptSearchs(ArrayList<NodeConceptSearch> nodeConceptSearchs) {
        this.nodeConceptSearchs = nodeConceptSearchs;
    }
    public boolean isIsSelectedItem() {
        return selectedItem;
    }

    public void setIsSelectedItem(boolean selectedItem) {
        this.selectedItem = selectedItem;
    }

    public boolean isExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }

    public boolean isWithNote() {
        return withNote;
    }

    public void setWithNote(boolean withNote) {
        this.withNote = withNote;
    }

    public boolean isWithId() {
        return withId;
    }

    public void setWithId(boolean withId) {
        this.withId = withId;
    }

    public boolean isIndexMatch() {
        return indexMatch;
    }

    public void setIndexMatch(boolean indexMatch) {
        this.indexMatch = indexMatch;
    }

    public boolean isIsSearchInSpecificTheso() {
        return isSearchInSpecificThesaurus;
    }

    public void setIsSearchInSpecificTheso(boolean isSearchInSpecificThesaurus) {
        this.isSearchInSpecificThesaurus = isSearchInSpecificThesaurus;
    }
}
