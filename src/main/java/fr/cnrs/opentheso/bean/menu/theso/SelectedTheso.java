package fr.cnrs.opentheso.bean.menu.theso;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.Thesaurus;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.alignment.ResultatAlignement;
import fr.cnrs.opentheso.bean.index.IndexSetting;
import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.bean.leftbody.viewconcepts.TreeConcepts;
import fr.cnrs.opentheso.bean.leftbody.viewgroups.TreeGroups;
import fr.cnrs.opentheso.bean.leftbody.viewliste.ListIndex;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.menu.connect.MenuBean;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.proposition.PropositionBean;
import fr.cnrs.opentheso.bean.rightbody.RightBodySetting;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.bean.rightbody.viewhome.ProjectBean;
import fr.cnrs.opentheso.bean.rightbody.viewhome.ViewEditorHomeBean;
import fr.cnrs.opentheso.bean.rightbody.viewhome.ViewEditorThesaurusHomeBean;
import fr.cnrs.opentheso.bean.search.SearchBean;
import fr.cnrs.opentheso.entites.UserGroupLabel;
import fr.cnrs.opentheso.repositories.CorpusLinkRepository;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.repositories.UserGroupLabelRepository;
import fr.cnrs.opentheso.services.ProjectService;
import fr.cnrs.opentheso.services.ThesaurusService;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.primefaces.PrimeFaces;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;


@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "selectedThesaurus")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SelectedTheso implements Serializable {

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final LanguageBean languageBean;
    private final IndexSetting indexSetting;
    private final TreeGroups treeGroups;
    private final TreeConcepts treeConcepts;
    private final Tree tree;
    private final ListIndex listIndex;
    private final ConceptView conceptBean;
    private final SearchBean searchBean;
    private final RoleOnThesaurusBean roleOnThesoBean;
    private final ViewEditorThesaurusHomeBean viewEditorThesoHomeBean;
    private final ViewEditorHomeBean viewEditorHomeBean;
    private final RightBodySetting rightBodySetting;
    private final MenuBean menuBean;
    private final PropositionBean propositionBean;
    private final CurrentUser currentUser;
    private final ProjectBean projectBean;
    private final CorpusLinkRepository corpusLinkRepository;
    private final LanguageRepository languageRepository;
    private final ThesaurusService thesaurusService;
    private final ProjectService projectService;
    private final UserGroupLabelRepository userGroupLabelRepository;

    private boolean fromUrl, isActionFromConcept, sortByNotation, isNetworkAvailable, isUriRequest, haveActiveCorpus;
    private String selectedIdTheso, currentIdTheso, optionThesoSelected, idConceptFromUri, idThesoFromUri, idGroupFromUri,
            thesoName, projectIdSelected, localUri, selectedLang, currentLang, arkId;

    private List<UserGroupLabel> projects, projectsList;
    private List<ResultatAlignement> resultAlignementList;
    private List<NodeLangTheso> nodeLangs;


    @PostConstruct
    public void initializing() {
        isNetworkAvailable = true;
        roleOnThesoBean.showListThesaurus(currentUser, currentIdTheso);
        sortByNotation = false;

        loadProject();
    }

    /**
     * capte les actions qui proviennent de la vue concept pour éviter
     * d'initialiser la vue ! sert pour replacer l'arbre
     */
    public void actionFromConceptToOn() {
        isActionFromConcept = true;
    }

    public void setSelectedOptionTheso() {
        switch(optionThesoSelected) {
            case "Option1":
                viewEditorHomeBean.reset();
                break;
            case "Option2":
                viewEditorHomeBean.initText();
                break;
            case "Option3":
                viewEditorHomeBean.initGoogleAnalytics();
                break;
        }
    }

    public String getUriOfTheso(Preferences nodePreference){

        var contextPath = FacesContext.getCurrentInstance().getExternalContext().getApplicationContextPath();
        var serverAdress = FacesContext.getCurrentInstance().getExternalContext().getRequestServerName();
        var protocole = FacesContext.getCurrentInstance().getExternalContext().getRequestScheme();
        var baseUrl = protocole + "://" + serverAdress + contextPath;

        if(nodePreference == null) {
            return baseUrl + "/?idt=" + currentIdTheso;
        } else {
            var thesaurus = thesaurusService.getThesaurusById(currentIdTheso);
            if(nodePreference.isOriginalUriIsArk()){
                String base = nodePreference.getOriginalUri();
                String ark = thesaurus.getIdArk();
                return base.replaceAll("/$", "") + "/" + ark.replaceAll("^/", "");
            } else {
                return (StringUtils.isEmpty(thesaurus.getIdArk()))
                        ? baseUrl + "/?idt=" + currentIdTheso
                        : baseUrl + "/api/ark:/" + thesaurus.getIdArk();
            }
        }
    }
    
    /**
     * Permet de charger le thésaurus sélectionné C'est le point d'entrée de
     * l'application
     */
    public void setSelectedTheso() throws IOException {

        haveActiveCorpus = CollectionUtils.isNotEmpty(corpusLinkRepository.findAllByIdThesaurusAndActive(getSelectedIdTheso(), true));
        
        viewEditorThesoHomeBean.reset();
        viewEditorHomeBean.reset();
        if (isUriRequest) {
            isUriRequest = false;
            searchBean.setNodeConceptSearchs(new ArrayList<>());
            menuBean.redirectToThesaurus();
            return;
        }

        if (StringUtils.isEmpty(selectedIdTheso)) {
            currentUser.resetUserPermissionsForThisThesaurus();
            treeGroups.reset();
            tree.reset();
            treeConcepts.reset();
            listIndex.reset();
            conceptBean.init();

            selectedLang = null;
            currentLang = null;
            nodeLangs = null;
            selectedIdTheso = null;
            currentIdTheso = null;
            thesoName = null;
            localUri = null;
            
            indexSetting.setIsSelectedTheso(false);
            indexSetting.setProjectSelected(!"-1".equals(projectIdSelected));

            projectBean.init();

            roleOnThesoBean.setSelectedThesaurusForSearch(new ArrayList<>());
            for (RoleOnThesaurusBean.ThesaurusModel thesoModel : roleOnThesoBean.getListThesaurus()) {
                roleOnThesoBean.getSelectedThesaurusForSearch().add(thesoModel.getId());
            }
            searchBean.setNodeConceptSearchs(new ArrayList<>());
            menuBean.redirectToThesaurus();
            return;
        }

        // après un raffraichissement F5
        if (selectedIdTheso.equalsIgnoreCase(currentIdTheso)) {
            if (!selectedLang.equalsIgnoreCase(currentLang)) {
                startNewLang();
            }

            searchBean.setNodeConceptSearchs(new ArrayList<>());
            menuBean.redirectToThesaurus();
            return;
        }

        sortByNotation = false;
        startNewTheso(null);
        currentUser.initUserPermissionsForThisTheso(selectedIdTheso);
        indexSetting.setIsSelectedTheso(true);
        indexSetting.setIsValueSelected(false);
        indexSetting.setIsHomeSelected(true);
        indexSetting.setIsThesoActive(true);
        
        propositionBean.searchNewPropositions();

        roleOnThesoBean.setUserRoleOnThisThesaurus(currentUser, currentIdTheso);

        for (RoleOnThesaurusBean.ThesaurusModel thesoModel : roleOnThesoBean.getListThesaurus()) {
            if (selectedIdTheso.equals(thesoModel.getId())) {
                roleOnThesoBean.setSelectedThesaurusForSearch(Collections.singletonList(selectedIdTheso));
            }
        }

        searchBean.setNodeConceptSearchs(new ArrayList<>());
        indexSetting.setProjectSelected(false);
        menuBean.redirectToThesaurus();
    }

    public void redirectToTheso() throws IOException{
        menuBean.redirectToThesaurus();
    }

    
    /**
     * Récupération de tous les projets en fonction de l'utilisateur :
     * - mode non connecté = on charge tous les projets
     * - mode connecté = on charge uniquement les projets de l'utilisateur
     */
    public void loadProject() {

        if (ObjectUtils.isEmpty(currentUser.getNodeUser())) {
            currentUser.initAllProject();
            currentUser.initAllTheso();
            projectIdSelected = ""+currentUser.getUserPermissions().getSelectedProject();
            selectedIdTheso = currentUser.getUserPermissions().getSelectedTheso();
            projectsList = projectService.findProjectByThesaurusStatus(false);
        } else {
            if (currentUser.getNodeUser().isSuperAdmin()) {
                projectsList = userGroupLabelRepository.findAll();
                projectsList.sort(Comparator.comparing(UserGroupLabel::getLabel, String.CASE_INSENSITIVE_ORDER));
            } else {
                var rows = userGroupLabelRepository.findProjectsByUserIdNative(currentUser.getNodeUser().getIdUser());
                projectsList = rows.stream()
                        .map(row -> new UserGroupLabel(((Number) row[0]).intValue(), (String) row[1]))
                        .collect(Collectors.toList());
            }
        }
        if(projectsList == null || projectsList.isEmpty()) {
            projectIdSelected = "-1";
        }
    }    
    
    public void setSelectedProject() {
        projectBean.setLangCodeSelected(languageBean.getIdLangue());
        if (CollectionUtils.isEmpty(projectBean.getAllLangs())) {
            projectBean.setAllLangs(languageRepository.findAll());
        }
        if ("-1".equals(projectIdSelected)) {
            currentUser.resetUserPermissionsForThisProject();
            currentUser.reloadAllThesoOfAllProject();
            roleOnThesoBean.showListThesaurus(currentUser, currentIdTheso);
            if(StringUtils.isEmpty(selectedIdTheso))
                indexSetting.setSelectedTheso(false);
            indexSetting.setProjectSelected(false);
        } else {
            currentUser.initUserPermissionsForThisProject(Integer.parseInt(projectIdSelected));
            projectBean.initProject(projectIdSelected, currentUser);

            if (!projectBean.getListeThesoOfProject().isEmpty()) {
                roleOnThesoBean.setAuthorizedThesaurus(projectBean.getListeThesoOfProject().stream()
                        .map(NodeIdValue::getId)
                        .collect(Collectors.toList()));
            } else {
                roleOnThesoBean.setAuthorizedThesaurus(Collections.emptyList());
            }
            roleOnThesoBean.addAuthorizedThesoToHM();
            roleOnThesoBean.setUserRoleOnThisThesaurus(currentUser, currentIdTheso);

            if (CollectionUtils.isNotEmpty(projectBean.getListeThesoOfProject())) {
                if (projectBean.getListeThesoOfProject().stream()
                        .filter(element -> StringUtils.equalsIgnoreCase(element.getId(), currentIdTheso))
                        .findFirst()
                        .isEmpty()) {
                    selectedIdTheso = null;
                    currentIdTheso = null;
                }
            } else {
                selectedIdTheso = null;
                currentIdTheso = null;
            }

            if (StringUtils.isEmpty(selectedIdTheso)) {
                indexSetting.setProjectSelected(true);
            }

            projectBean.init();
        }
    }
    
    public void setSelectedThesaurusForSearch() throws IOException {

        viewEditorThesoHomeBean.reset();
        viewEditorHomeBean.reset();

        // après un raffraichissement F5
        if (selectedIdTheso.equalsIgnoreCase(currentIdTheso)) {
            if (!selectedLang.equalsIgnoreCase(currentLang)) {
                startNewLang();
            }
            return;
        }

        sortByNotation = false;
        startNewTheso(null);
        indexSetting.setIsSelectedTheso(true);
        indexSetting.setIsValueSelected(true);
        indexSetting.setIsHomeSelected(false);
        indexSetting.setIsThesoActive(true);
        indexSetting.setProjectSelected(false);
    }

    /**
     * permet de recharger l'arbre des collections 
     */
    public void reloadGroups(){
        treeGroups.reset();
        treeGroups.initialise(selectedIdTheso, selectedLang);   
        rightBodySetting.setIndex("1");
        PrimeFaces.current().executeScript("window.location.reload();");
    }
    
    /**
     * permet de recharger l'arbre des collections 
     */
    public void reloadConceptTree(){
        treeConcepts.reset();
        treeConcepts.initialise(selectedIdTheso, selectedLang);        
    }    
    
    /**
     * Permet de Re-charger le thésaurus sélectionné, pour activer des mises à jour non prises en compte
     */
    public void reloadSelectedTheso() throws IOException {
        loadProject();
        roleOnThesoBean.showListThesaurus(currentUser, currentIdTheso);

        searchBean.reset();
        viewEditorThesoHomeBean.reset();
        viewEditorHomeBean.reset();
        treeGroups.reset();

        if (selectedIdTheso == null || selectedIdTheso.isEmpty()) {
            return;
        }
        startNewTheso(null);
        tree.setIdConceptSelected(null);

        indexSetting.setIsSelectedTheso(true);
        indexSetting.setIsValueSelected(false);
        indexSetting.setIsHomeSelected(true);

        menuBean.redirectToThesaurus();

        PrimeFaces.current().executeScript("window.location.reload();");
    }

    /**
     * permet de changer la langue du thésaurus et recharger les données
     */
    public void changeLang() {
        if (isUriRequest) {
            isUriRequest = false;
            return;
        }
        if (selectedLang == null || selectedIdTheso == null) {
            return;
        }

        if (selectedLang.equalsIgnoreCase(currentLang)) {
            isActionFromConcept = false;
            return;
        }
        if (selectedLang.equalsIgnoreCase("all")) {
            isActionFromConcept = false;
            return;
        }
        try {
            startNewLang();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * initialise le nouveau thésaurus avec l'identifiant de thésaurus
     * sélectionné si la langue est fournie, on initialise dans cette langue,
     * sinon, on prend la langue source du thésaurus
     */
    private void startNewTheso(String idLang) {
        currentIdTheso = selectedIdTheso;
        // setting des préférences du thésaurus sélectionné
        roleOnThesoBean.initNodePref(this);
        if (StringUtils.isEmpty(idLang)) {
            idLang = getIdLang();
        }
        if (StringUtils.isEmpty(idLang)) {
            return;
        }

        nodeLangs = thesaurusService.getAllUsedLanguagesOfThesaurusNode(selectedIdTheso, languageBean.getIdLangue());

        currentLang = idLang;
        selectedLang = idLang;
        thesoName = thesaurusService.getTitleOfThesaurus(selectedIdTheso, selectedLang);
        Thesaurus thesaurus = thesaurusService.getThesaurusById(selectedIdTheso);
        if (thesaurus != null && StringUtils.isNotEmpty(thesaurus.getIdArk())) {
            arkId = thesaurus.getIdArk();
        } else {
            arkId = null;
        }
        // initialisation de l'arbre des groupes
        treeGroups.reset();
        treeGroups.initialise(selectedIdTheso, selectedLang);

        treeConcepts.reset();
        treeConcepts.initialise(selectedIdTheso, selectedLang);

        tree.reset();
        tree.initialise(selectedIdTheso, selectedLang);

        listIndex.reset();
        conceptBean.init();
    }

    private void startNewLang() throws IOException {
        currentLang = selectedLang;
        
        treeGroups.reset();
        tree.reset();
        listIndex.reset();

        // initialisation de l'arbre des groupes
        treeGroups.initialise(selectedIdTheso, selectedLang);
        treeConcepts.initialise(selectedIdTheso, selectedLang);
        tree.initialise(selectedIdTheso, selectedLang);
        
        if (!isActionFromConcept) {
            conceptBean.init();
        }
        isActionFromConcept = false;

        searchBean.onSelectConcept(selectedIdTheso, tree.getIdConceptSelected(), selectedLang);

        PrimeFaces.current().executeScript("srollToSelected()");
        searchBean.setNodeConceptSearchs(new ArrayList<>());
    }

    private String getIdLang() {
        String idLang = workLanguage;
        if (roleOnThesoBean.getNodePreference() != null) {
            idLang = roleOnThesoBean.getNodePreference().getSourceLang();
        }
        return idLang;
    }

    /**
     * Pour sélectionner un thésaurus ou un concept en passant par l'URL
     */
    public void preRenderView() {
        if (StringUtils.isEmpty(idThesoFromUri)) {
            fromUrl = false;
            return;
        }

        fromUrl = true;
        if (StringUtils.isEmpty(idThesoFromUri)) {
            fromUrl = false;
            return;
        }

        fromUrl = true;
        if (idThesoFromUri.equalsIgnoreCase(selectedIdTheso)) {
            if (StringUtils.isEmpty(idConceptFromUri)) {
                //test si c'est une collection
                idConceptFromUri = null;
                idThesoFromUri = null;
                if (StringUtils.isNotEmpty(idGroupFromUri)) {
                    // sélectionner le groupe ou collection
                    treeGroups.selectThisGroup(idGroupFromUri.trim());
                    return;
                }

            }
            if (StringUtils.isEmpty(currentLang)) {
                String idLang = getIdLang();
                if (StringUtils.isEmpty(idLang)) {
                    return;
                }
                currentLang = idLang;
                selectedLang = idLang;
            }
            haveActiveCorpus = CollectionUtils.isNotEmpty(corpusLinkRepository.findAllByIdThesaurusAndActive(getSelectedIdTheso(), true));
            conceptBean.getConcept(selectedIdTheso, idConceptFromUri, currentLang, currentUser);
            isActionFromConcept = true;
            idConceptFromUri = null;
            idThesoFromUri = null;
            thesoName = thesaurusService.getTitleOfThesaurus(selectedIdTheso, selectedLang);
            return;
        } else {
            // gestion de l'accès par thésaurus d'un identifiant différent
            var thesaurus = thesaurusService.getThesaurusById(idThesoFromUri);
            if (thesaurus != null && !thesaurus.getIsPrivate()) {
                currentUser.resetUserPermissionsForThisThesaurus();
                /// chargement du thésaurus
                selectedIdTheso = idThesoFromUri;
                roleOnThesoBean.initNodePref(selectedIdTheso);
                startNewTheso(roleOnThesoBean.getNodePreference().getSourceLang());//currentLang);
                indexSetting.setIsSelectedTheso(true);
                indexSetting.setIsThesoActive(true);
                rightBodySetting.setIndex("0");
                haveActiveCorpus = CollectionUtils.isNotEmpty(corpusLinkRepository.findAllByIdThesaurusAndActive(getSelectedIdTheso(), true));
                if (idConceptFromUri != null && !idConceptFromUri.isEmpty()) {
                    // chargement du concept puisqu'il est renseigné
                    conceptBean.getConcept(currentIdTheso, idConceptFromUri, currentLang, currentUser);
                    isActionFromConcept = true;
                    
                } else {
                    //cas d'appel pour une collection
                    if(StringUtils.isNotEmpty(idGroupFromUri)) {
                        treeGroups.selectThisGroup(idGroupFromUri.trim());
                        rightBodySetting.setIndex("1");
                    } else {
                        indexSetting.setIsHomeSelected(true);
                        conceptBean.setNodeConcept(null);
                    }
                }
            } else {
                return;
            }
        }
        haveActiveCorpus = CollectionUtils.isNotEmpty(corpusLinkRepository.findAllByIdThesaurusAndActive(getSelectedIdTheso(), true));
        currentUser.initUserPermissionsForThisTheso(selectedIdTheso);
        idConceptFromUri = null;
        idThesoFromUri = null;
    }

}
