package fr.cnrs.opentheso.bean.menu.users;

import fr.cnrs.opentheso.bean.leftbody.viewgroups.TreeGroups;
import fr.cnrs.opentheso.bean.leftbody.viewtree.Tree;
import fr.cnrs.opentheso.bean.rightbody.viewconcept.ConceptView;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.group.NodeGroup;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.models.users.NodeUserRoleGroup;
import fr.cnrs.opentheso.models.userpermissions.NodeProjectThesoRole;
import fr.cnrs.opentheso.models.userpermissions.NodeThesoRole;
import fr.cnrs.opentheso.models.userpermissions.UserPermissions;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ProjectService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.UserRoleGroupService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.bean.index.IndexSetting;
import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.bean.menu.connect.MenuBean;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.proposition.PropositionBean;
import fr.cnrs.opentheso.bean.rightbody.RightBodySetting;
import fr.cnrs.opentheso.bean.rightbody.viewhome.ProjectBean;
import fr.cnrs.opentheso.bean.rightbody.viewhome.ViewEditorHomeBean;
import fr.cnrs.opentheso.bean.search.SearchBean;
import fr.cnrs.opentheso.entites.UserGroupLabel;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import fr.cnrs.opentheso.v2.user.policy.RoleLabels;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import jakarta.servlet.http.HttpSession;

@Slf4j
@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "currentUser")
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CurrentUser implements Serializable {

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final ConceptView conceptView;
    private final GroupService groupService;
    private final ConceptService conceptService;
    private final Tree tree;
    private final MenuBean menuBean;
    private final SearchBean searchBean;
    private final TreeGroups treeGroups;
    private final ProjectBean projectBean;
    private final LanguageBean languageBean;
    private final IndexSetting indexSetting;
    private final SelectedTheso selectedTheso;
    private final PropositionBean propositionBean;
    private final RoleOnThesaurusBean roleOnThesaurusBean;
    private final RightBodySetting rightBodySetting;
    private final ViewEditorHomeBean viewEditorHomeBean;
    private final ProjectService projectService;
    private final UserService userService;
    private final ThesaurusService thesaurusService;
    private final PreferenceService preferenceService;
    private final UserRoleGroupService userRoleGroupService;

    private NodeUser nodeUser;
    private String username, password;
    private boolean ldapEnable;
    private UserPermissions userPermissions;
    private List<NodeUserRoleGroup> allAuthorizedProjectAsAdmin;
    //pour KeyCloak
    private boolean keyCloak = false;
    private String mail;



    @PostConstruct
    public void init() {
        checkAndLoginFromSso();
    }

    public void onPageLoad() {
        HttpSession session = (HttpSession) FacesContext.getCurrentInstance()
                .getExternalContext().getSession(false);

        if (session == null) return;

        // Si déjà traité dans cette session, on ne fait rien
        if (Boolean.TRUE.equals(session.getAttribute("ssoProcessed"))) return; // déjà traité

        Integer ssoUserId = (Integer) session.getAttribute("ssoUserId");
        if (ssoUserId == null) return;

        // Marquer comme traité AVANT le login (évite les doubles appels)
        session.setAttribute("ssoProcessed", true);
        session.removeAttribute("ssoUserId");

        userService.findById(ssoUserId).ifPresent(user -> {
            try {
                // Si un utilisateur différent est déjà connecté → déconnexion propre d'abord
                if (nodeUser != null && nodeUser.getIdUser() != user.getId().intValue()){
                    log.info("SSO : changement d'utilisateur {} → {}", nodeUser.getName(), user.getUsername());
                    disconnectForSso();
                    loginWithUser(user);
                } else if (nodeUser == null) {
                    // Pas d'utilisateur connecté → login direct
                    loginWithUser(user);
                } else {
                    // Même utilisateur déjà connecté → on ne fait rien
                    log.info("SSO : utilisateur {} déjà connecté, pas de reconnexion", user.getUsername());
                }
            } catch (Exception e) {
                log.error("Erreur SSO login", e);
            }
        });
    }
    private void disconnectForSso() throws IOException{
        if (nodeUser == null) return;

        nodeUser = null;

        // Réinitialisation des permissions
        resetPermissionsAfterLogout();

        // Réinitialisation de l'état
        roleOnThesaurusBean.setAndClearThesoInAuthorizedList(selectedTheso);
        rightBodySetting.setIndex("0");
        viewEditorHomeBean.reset();

        // Réinitialisation du projet
        selectedTheso.loadProject();
        selectedTheso.setSelectedProject();

        roleOnThesaurusBean.setAuthorizedThesaurus(Collections.emptyList());

        log.info("SSO : session réinitialisée pour nouveau login");
    }


    // pour capter les connexions qui arrivent du SSO via un Bouton et clé d'API
    private void checkAndLoginFromSso() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext == null) {
            // Appelé hors requête JSF (ex. callback OAuth2 Spring Security) → ignorer.
            return;
        }
        HttpSession session = (HttpSession) facesContext.getExternalContext().getSession(false);

        if (session != null) {
            Integer ssoUserId = (Integer) session.getAttribute("ssoUserId");
            if (ssoUserId != null) {
                session.removeAttribute("ssoUserId"); // usage unique
                userService.findById(ssoUserId).ifPresent(user -> {
                    try {
                        loginWithUser(user);
                    } catch (Exception e) {
                        log.error("Erreur SSO login", e);
                    }
                });
            }
        }
    }


    // gestion des messages d'erreurs de KeyCloak'
    public String getLoginError() {
        // Lire la session à chaque rendu
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = fc.getExternalContext().getSessionMap();

        String messageError = (String) sessionMap.remove("LOGIN_ERROR_MESSAGE");
        String messageInfo = (String) sessionMap.remove("LOGIN_INFO_MESSAGE");

        if (messageError != null && !messageError.isEmpty()) {
            // Ajouter le message global pour PrimeFaces growl
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Connexion impossible",
                    messageError));
        }
        if (messageInfo != null && !messageInfo.isEmpty()) {
            // Ajouter le message global pour PrimeFaces growl
            fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "",
                    messageInfo));
        }
        return "";
    }

    public void clear() {
        if (allAuthorizedProjectAsAdmin != null) {
            allAuthorizedProjectAsAdmin.clear();
            allAuthorizedProjectAsAdmin = null;
        }
        nodeUser = null;
        username = null;
        password = null;
    }

    public void disconnect() throws IOException {

        if(nodeUser == null) return;

        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, languageBean.getMsg("connect.goodbye"), nodeUser.getName());
        FacesContext.getCurrentInstance().addMessage(null, facesMessage);

        nodeUser = null;

        // initialisation des permissions
        resetPermissionsAfterLogout();
        
        // tester si le thésaurus en cours est privé, alors après une déconnexion, on devrait plus l'afficher
        roleOnThesaurusBean.setAndClearThesoInAuthorizedList(selectedTheso);
        indexSetting.setIsThesoActive(true);
        rightBodySetting.setIndex("0");

        viewEditorHomeBean.reset();

        selectedTheso.loadProject();
        selectedTheso.setSelectedProject();

        if ("-1".equals(selectedTheso.getProjectIdSelected())) {
            roleOnThesaurusBean.setPublicThesaurus(this, selectedTheso.getCurrentIdTheso());
            if(StringUtils.isNotEmpty(selectedTheso.getCurrentIdTheso())){
                var thesaurus = thesaurusService.getThesaurusById(selectedTheso.getCurrentIdTheso());
                if (thesaurus.getIsPrivate()) {
                    selectedTheso.setCurrentIdTheso(null);
                    indexSetting.setSelectedTheso(false);
                } else {
                    indexSetting.setSelectedTheso(true);
                }
                indexSetting.setProjectSelected(false);
            }
        } else if (selectedTheso.getProjectsList().stream()
                .filter(element -> element.getId() == Integer.parseInt(selectedTheso.getProjectIdSelected()))
                .findFirst().isEmpty()) {
            selectedTheso.setProjectIdSelected("-1");
            indexSetting.setProjectSelected(false);
            selectedTheso.setSelectedIdTheso(null);
            indexSetting.setSelectedTheso(false);
        } else {
            if (!projectBean.getListeThesoOfProject().isEmpty()) {
                roleOnThesaurusBean.setAuthorizedThesaurus(projectBean.getListeThesoOfProject().stream()
                        .map(NodeIdValue::getId)
                        .collect(Collectors.toList()));
            } else {
                roleOnThesaurusBean.setAuthorizedThesaurus(Collections.emptyList());
            }
            roleOnThesaurusBean.addAuthorizedThesoToHM();
            roleOnThesaurusBean.setUserRoleOnThisThesaurus(this, selectedTheso.getCurrentIdTheso());

            var thesaurus = thesaurusService.getThesaurusById(selectedTheso.getCurrentIdTheso());
            if (StringUtils.isNotEmpty(selectedTheso.getCurrentIdTheso()) && thesaurus.getIsPrivate()) {
                indexSetting.setSelectedTheso(true);
                indexSetting.setProjectSelected(false);
            }
        }

        treeGroups.initialise(selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());
        tree.loadConceptTree();

        var groups = groupService.getListGroupOfConcept(selectedTheso.getCurrentIdTheso(),
                conceptView.getNodeFullConcept().getIdentifier(), selectedTheso.getCurrentLang());
        try {
            if (CollectionUtils.isNotEmpty(groups) && groups.stream().anyMatch(NodeGroup::isGroupPrivate)) {
                indexSetting.setIsSelectedTheso(true);
                indexSetting.setIsValueSelected(false);
                indexSetting.setIsHomeSelected(true);
                indexSetting.setIsThesoActive(true);
                indexSetting.setProjectSelected(false);
            }
        } catch (Exception e) {
            log.error("Erreur d'initialisation des groupes privés", e.getMessage());
        }

        conceptView.setNodeFullConcept(conceptService.getConcept(conceptView.getIdConceptSelected(),
                selectedTheso.getCurrentIdTheso(), conceptView.getSelectedLang(), 0, 1, false));
        tree.reloadSelectedConcept();

        if (!"index".equals(menuBean.getActivePageName())) {
            menuBean.redirectToThesaurus();
        } else {
            PrimeFaces.current().ajax().update("containerIndex:contentConcept");
            PrimeFaces.current().ajax().update("containerIndex:searchBar");
            PrimeFaces.current().ajax().update("containerIndex:header");
            PrimeFaces.current().ajax().update("containerIndex:formLeftTab");
            PrimeFaces.current().ajax().update("menuBar");
        }
    }

    public void setUser(User user) {
        nodeUser = new NodeUser(user.getId(), user.getUsername(), user.getMail(), user.getInstitution(), user.getActive(),
                user.getAlertMail(), user.getIsSuperAdmin(), user.getPassToModify(),
                user.getApiKey(), user.getKeyNeverExpire(), user.getKeyExpiresAt(),
                user.getIsServiceAccount(), user.getKeyDescription());
    }

    /**
     * Connect l'utilisateur si son compte en récupérant toutes les informations
     * lui concernant
     * le lien de l'index si le compte existe, un message d'erreur sinon init
     * c'est une parametre que viens du "isUserExist" ou return une 1 si on fait
     * le login normal (usuaire, pass), une 2 si on fait le login avec le
     * motpasstemp (et nous sommes dirigées a la page web de changer le
     * motpasstemp) #MR
     */
    public void login() throws Exception {

        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            showErrorMessage("champ vide non autorisé");
            return;
        }

        User user = null;
        if (keyCloak) {
            Optional<User> user2 = userService.findByMail(mail);
            if(user2.isPresent()) {
                user = user2.get();
            }
        } else {
            // Passage en BCrypt
            user = userService.findByUsernameAndPasswordMigrated(username, password);
        }

        if (user == null) {
            // si la connexion échoue avec pseudo, on teste avec l'email
            user = userService.findByMailAndPasswordMigrated(username, password);
            if (user == null) {
                showErrorMessage("User or password wrong, please try again");
                return;
            }
        }

        // on récupère le compte de l'utilisateur
        nodeUser = new NodeUser(user.getId(), user.getUsername(), user.getMail(), user.getInstitution(), user.getActive(),
                user.getAlertMail(), user.getIsSuperAdmin(), user.getPassToModify(),
                user.getApiKey(), user.getKeyNeverExpire(), user.getKeyExpiresAt(),
                user.getIsServiceAccount(), user.getKeyDescription());

        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_INFO, languageBean.getMsg("connect.welcome"), username);
        FacesContext.getCurrentInstance().addMessage(null, facesMessage);

        if ("index".equals(menuBean.getActivePageName())) {
            menuBean.setNotificationPanelVisible(true);
        }

        setInfos();
        //// Nouvelle gestion des droits pour l'utilisateur avec l'Objet UserPermissions

        if ("2".equals(rightBodySetting.getIndex())) {
            rightBodySetting.setIndex("0");
        }

        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);

        selectedTheso.loadProject();

        if ("-1".equals(selectedTheso.getProjectIdSelected()) || StringUtils.isEmpty(selectedTheso.getProjectIdSelected())) {
            indexSetting.setProjectSelected(false);
            if(!StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())){
                var thesaurus = thesaurusService.getThesaurusById(selectedTheso.getCurrentIdTheso());
                if (thesaurus.getIsPrivate()) {
                    selectedTheso.setCurrentIdTheso(null);
                    indexSetting.setSelectedTheso(false);
                } else {
                    indexSetting.setSelectedTheso(true);
                }
            }
        } else {
            projectBean.initProject(selectedTheso.getProjectIdSelected(), this);

            if (!projectBean.getListeThesoOfProject().isEmpty()) {
                roleOnThesaurusBean.setAuthorizedThesaurus(projectBean.getListeThesoOfProject().stream()
                        .map(NodeIdValue::getId)
                        .collect(Collectors.toList()));
            } else {
                roleOnThesaurusBean.setAuthorizedThesaurus(Collections.emptyList());
            }
            roleOnThesaurusBean.addAuthorizedThesoToHM();
            roleOnThesaurusBean.setUserRoleOnThisThesaurus(this, selectedTheso.getCurrentIdTheso());
            projectBean.init();
        }

        treeGroups.initialise(selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());
        tree.loadConceptTree();

        conceptView.setNodeFullConcept(conceptService.getConcept(conceptView.getIdConceptSelected(),
                selectedTheso.getCurrentIdTheso(), conceptView.getSelectedLang(), 0, 1, true));
        tree.reloadSelectedConcept();

        PrimeFaces.current().executeScript("PF('login').hide();");

        PrimeFaces.current().ajax().update("idLogin");
        PrimeFaces.current().ajax().update("containerIndex:header");
        PrimeFaces.current().ajax().update("containerIndex:formLeftTab");
    }


    // Nouvelle méthode appelable depuis le filtre SSO
    private void loginWithUser(User user) throws Exception {
        nodeUser = new NodeUser(user.getId(), user.getUsername(), user.getMail(),
                user.getInstitution(), user.getActive(),
                user.getAlertMail(), user.getIsSuperAdmin(), user.getPassToModify(),
                user.getApiKey(), user.getKeyNeverExpire(), user.getKeyExpiresAt(),
                user.getIsServiceAccount(), user.getKeyDescription());

        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_INFO,
                languageBean.getMsg("connect.welcome"), user.getUsername());
        FacesContext.getCurrentInstance().addMessage(null, facesMessage);

        if ("index".equals(menuBean.getActivePageName())) {
            menuBean.setNotificationPanelVisible(true);
        }

        setInfos();

        if ("2".equals(rightBodySetting.getIndex())) {
            rightBodySetting.setIndex("0");
        }

        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);

        selectedTheso.loadProject();

        if ("-1".equals(selectedTheso.getProjectIdSelected()) ||
                StringUtils.isEmpty(selectedTheso.getProjectIdSelected())) {
            indexSetting.setProjectSelected(false);
            if (!StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
                var thesaurus = thesaurusService.getThesaurusById(selectedTheso.getCurrentIdTheso());
                if (thesaurus.getIsPrivate()) {
                    selectedTheso.setCurrentIdTheso(null);
                    indexSetting.setSelectedTheso(false);
                } else {
                    indexSetting.setSelectedTheso(true);
                }
            }
        } else {
            projectBean.initProject(selectedTheso.getProjectIdSelected(), this);

            if (!projectBean.getListeThesoOfProject().isEmpty()) {
                roleOnThesaurusBean.setAuthorizedThesaurus(projectBean.getListeThesoOfProject().stream()
                        .map(NodeIdValue::getId)
                        .collect(Collectors.toList()));
            } else {
                roleOnThesaurusBean.setAuthorizedThesaurus(Collections.emptyList());
            }
            roleOnThesaurusBean.addAuthorizedThesoToHM();
            roleOnThesaurusBean.setUserRoleOnThisThesaurus(this, selectedTheso.getCurrentIdTheso());
            projectBean.init();
        }

        treeGroups.initialise(selectedTheso.getCurrentIdTheso(), selectedTheso.getCurrentLang());
        tree.loadConceptTree();

        conceptView.setNodeFullConcept(conceptService.getConcept(conceptView.getIdConceptSelected(),
                selectedTheso.getCurrentIdTheso(), conceptView.getSelectedLang(), 0, 1, true));
        tree.reloadSelectedConcept();

        PrimeFaces.current().executeScript("PF('login').hide();");
        PrimeFaces.current().ajax().update("idLogin");
        PrimeFaces.current().ajax().update("containerIndex:header");
        PrimeFaces.current().ajax().update("containerIndex:formLeftTab");
    }


    /**
     * initialisation des permissions suivant l'utilisateur connecté 
     */
    public void initUserPermissions(){
        if(nodeUser == null) return;
        if(userPermissions == null){
            userPermissions = new UserPermissions();
        }

        // liste des projets de l'utilisateur
        if (nodeUser.isSuperAdmin()) {
            userPermissions.setListProjects(userRoleGroupService.findAllUserRoleGroup());
        } else {  
            userPermissions.setListProjects(userService.getProjectOfUser(nodeUser.getIdUser()));
            setListProjectForUser();
        }
        setAllListThesoOfAllProject();
    }
    
    private void setAllListThesoOfAllProject(){
        // liste des thésaurus de l'utilisateur (tous les droits en partant du contributeur)
        if (nodeUser.isSuperAdmin()) {
            userPermissions.setListThesaurus(thesaurusService.getAllThesaurus(true));
            userPermissions.setRole(1);
            userPermissions.setRoleName("superAdmin");   
            if(userPermissions.getSelectedProject() != -1) {
                initUserPermissionsForThisProject(userPermissions.getSelectedProject());
            }
        } else {  
            // les projets de l'utilisateurs avec les roles sur les thésaurus par projet
            List<NodeProjectThesoRole> nodeProjectThesoRoles = new ArrayList<>();

            for (UserGroupLabel userGroupLabel : userPermissions.getListProjects()) {
                NodeProjectThesoRole nodeProjectThesoRole = new NodeProjectThesoRole();
                nodeProjectThesoRole.setIdProject(userGroupLabel.getId()); // id du projet
                nodeProjectThesoRole.setProjectName(userGroupLabel.getLabel()); // label du projet

                List<NodeThesoRole> nodeThesoRoles = userService.getAllRolesThesosByUserGroup(nodeProjectThesoRole.getIdProject(), nodeUser.getIdUser());

                nodeProjectThesoRole.setNodeThesoRoles(nodeThesoRoles);
                nodeProjectThesoRoles.add(nodeProjectThesoRole);
            }
            userPermissions.setNodeProjectsWithThesosRoles(nodeProjectThesoRoles);
            setListThesaurusForUser();
        }        
    }
    
    private void setListProjectForUser(){
        if(userPermissions.getSelectedProject() == -1) return;

        for (UserGroupLabel userGroupLabel : userPermissions.getListProjects()) {
            if(userPermissions.getSelectedProject() == userGroupLabel.getId()) return;
        }
        resetUserPermissionsForThisProject();
        resetSelectedTheso();
    }  

    private void resetSelectedTheso(){
        userPermissions.setSelectedTheso(null);
        userPermissions.setSelectedThesoName("");
        userPermissions.setPreferredLangOfSelectedTheso(null);
        userPermissions.setListLangsOfSelectedTheso(null);
        userPermissions.setProjectOfselectedTheso(-1);
        userPermissions.setProjectOfselectedThesoName("");
    }
    
    private void setListThesaurusForUser(){
        boolean resetTheso = true;
        List<NodeIdValue> thesaurusList = new ArrayList<>();
        for (NodeProjectThesoRole nodeProjectsWithThesosRole : userPermissions.getNodeProjectsWithThesosRoles()) {
            if(userPermissions.getSelectedProject() == -1) {
                for (NodeThesoRole nodeThesoRole : nodeProjectsWithThesosRole.getNodeThesoRoles()) {
                    NodeIdValue nodeIdValue = new NodeIdValue();
                    nodeIdValue.setId(nodeThesoRole.getIdTheso());
                    nodeIdValue.setValue(nodeThesoRole.getThesoName());
                    if(!StringUtils.isEmpty(userPermissions.getSelectedTheso())) {
                        if(userPermissions.getSelectedTheso().equalsIgnoreCase(nodeThesoRole.getIdTheso())) {
                            resetTheso = false;
                        }
                    }
                    thesaurusList.add(nodeIdValue);
                } 
            }  else {          
                if(userPermissions.getSelectedProject() == nodeProjectsWithThesosRole.getIdProject()) {
                    for (NodeThesoRole nodeThesoRole : nodeProjectsWithThesosRole.getNodeThesoRoles()) {
                        NodeIdValue nodeIdValue = new NodeIdValue();
                        nodeIdValue.setId(nodeThesoRole.getIdTheso());
                        nodeIdValue.setValue(nodeThesoRole.getThesoName());
                        if(!StringUtils.isEmpty(userPermissions.getSelectedTheso())) {
                            if(userPermissions.getSelectedTheso().equalsIgnoreCase(nodeThesoRole.getIdTheso())) {
                                resetTheso = false;
                            }
                        }
                        thesaurusList.add(nodeIdValue);
                    } 
                }
            }
        }

        userPermissions.setListThesaurus(thesaurusList);
        if(resetTheso) {
            resetUserPermissionsForThisThesaurus();
            resetUserPermissionsForThisProject();
        }
        else
            initUserPermissionsForThisTheso(userPermissions.getSelectedTheso());
    }
    
    public void initUserPermissionsForThisTheso(String idTheso){
        if(userPermissions == null){
            userPermissions = new UserPermissions();
        }
        if(StringUtils.isEmpty(idTheso)) {
            resetUserPermissionsForThisThesaurus();
            return;
        }

        int idProject, idRole; 
        userPermissions.setSelectedTheso(idTheso);
        userPermissions.setPreferredLangOfSelectedTheso(preferenceService.getWorkLanguageOfThesaurus(selectedTheso.getCurrentIdTheso()));
        userPermissions.setSelectedThesoName(thesaurusService.getTitleOfThesaurus(idTheso, userPermissions.getPreferredLangOfSelectedTheso()));
        
        
        userPermissions.setListLangsOfSelectedTheso(thesaurusService.getAllUsedLanguagesOfThesaurusNode(
                selectedTheso.getCurrentIdTheso(), userPermissions.getPreferredLangOfSelectedTheso()));
        
        idProject = userService.getGroupOfThisThesaurus(selectedTheso.getCurrentIdTheso());
        
        if(nodeUser != null) {
            if(nodeUser.isSuperAdmin()) {
                userPermissions.setRole(1);
                userPermissions.setRoleName("superAdmin");                
            } else {
                idRole = userService.getRoleOnThisThesaurus(nodeUser.getIdUser(), idProject, idTheso);
                userPermissions.setRole(idRole);
                userPermissions.setRoleName(RoleLabels.fromRoleId(idRole));
            }
        }

        userPermissions.setProjectOfselectedTheso(idProject);
        if (idProject != -1) {
            userPermissions.setProjectOfselectedThesoName(userRoleGroupService.getUserGroupLabelRepository(idProject).getLabel());
        }
    }
    
    public void initUserPermissionsForThisProject(int idProject){
        if(userPermissions == null){
            userPermissions = new UserPermissions();
        }

        userPermissions.setSelectedProject(idProject);
        userPermissions.setSelectedProjectName(userRoleGroupService.getUserGroupLabelRepository(idProject).getLabel());
        userPermissions.setListThesaurus(userService.getThesaurusOfProject(idProject, workLanguage, nodeUser != null));
        if(!StringUtils.isEmpty(userPermissions.getSelectedTheso())){
            for (NodeIdValue nodeIdValue : userPermissions.getListThesaurus()) {
                if(nodeIdValue.getId().equalsIgnoreCase(userPermissions.getSelectedTheso()))
                    return;
            }
            resetUserPermissionsForThisThesaurus();
        }
    }
    
    /**
     * remise à zéro des variables pour le projet en cours
     */    
    public void resetUserPermissionsForThisThesaurus(){
        if(userPermissions == null){
            userPermissions = new UserPermissions();
        }
        userPermissions.setRole(-1);
        userPermissions.setRoleName("");
    }       
    
    /**
     * remise à zéro des variables pour le projet en cours
     */
    public void resetUserPermissionsForThisProject(){
        if(userPermissions == null){
            userPermissions = new UserPermissions();
        }           
        userPermissions.setSelectedProject(-1);
        userPermissions.setSelectedProjectName("");
    }      
    
    /**
     * initialisation des toutes les permissions vers un droit public
     */
    private void resetPermissionsAfterLogout(){
        if(userPermissions == null) return;
        userPermissions.setNodeProjectsWithThesosRoles(null);
        
        resetUserPermissionsForThisThesaurus();
    }
    
    /**
     * Chargement de tous les projets publics
     */
    public void initAllProject(){
        if(userPermissions == null){
            userPermissions = new UserPermissions();
        }
        userPermissions.setListProjects(projectService.findProjectByThesaurusStatus(false));
        
        // contrôle si le projet actuel est dans la liste, sinon, on initialise le projet sélectionné à -1
        if(userPermissions.getSelectedProject() != -1){
            for (UserGroupLabel listProject : userPermissions.getListProjects()) {
                if(listProject.getId() == userPermissions.getSelectedProject()){
                    return;
                } 
            }
            userPermissions.setSelectedProject(-1);
            userPermissions.setSelectedProjectName("");
        }
        if(userPermissions.getListProjects() == null || userPermissions.getListProjects().isEmpty()) {
            resetUserPermissionsForThisProject();
        }    
    }
    /**
     * Rcharge tous les thésaurus de tous les projets après avoir sélectionné (tous les projets)  
     */
    public void reloadAllThesoOfAllProject(){
        if(nodeUser == null) {
            initAllTheso();
        } else {
            setAllListThesoOfAllProject();      
        }
    }
    
    /**
     * chargement de tous les thésaurus publics
     */
    public void initAllTheso(){
        if(userPermissions == null){
            userPermissions = new UserPermissions();
        }

        userPermissions.setListThesaurus(thesaurusService.getAllThesaurus(nodeUser != null));

        // contrôle si le thésaurus actuel est dans la liste, sinon, on initialise le thésaurus à null
        if(!StringUtils.isEmpty(userPermissions.getSelectedTheso())){
            for (NodeIdValue nodeIdValue : userPermissions.getListThesaurus()) {
                if(nodeIdValue.getId().equalsIgnoreCase(userPermissions.getSelectedTheso())){
                    return;
                } 
            }
            userPermissions.setSelectedTheso(null);
            userPermissions.setSelectedProjectName("");
        }         
        if(userPermissions.getListThesaurus() == null || userPermissions.getListThesaurus().isEmpty()) {
            resetUserPermissionsForThisThesaurus();
        }   
    }

    public boolean isHasRoleAsContributor(){
        return ObjectUtils.isNotEmpty(nodeUser) && ObjectUtils.isNotEmpty(userPermissions) &&
                (
                (userPermissions.isContributor()) || (userPermissions.isManager()) || (userPermissions.isAdmin()) || (nodeUser.isSuperAdmin())
                ) ;
    }

    public boolean isHasRoleAsManager(){
        return ObjectUtils.isNotEmpty(nodeUser) && ObjectUtils.isNotEmpty(userPermissions) &&
                (
                (userPermissions.isManager()) || (userPermissions.isAdmin()) || (nodeUser.isSuperAdmin())
                );
    }
    
    public boolean isHasRoleAsAdmin(){
        return ObjectUtils.isNotEmpty(nodeUser) && ObjectUtils.isNotEmpty(userPermissions) && ((userPermissions.isAdmin()) || (nodeUser.isSuperAdmin()));
    }

    public boolean isHasRoleAsSuperAdmin(){
        return ObjectUtils.isNotEmpty(nodeUser) && ObjectUtils.isNotEmpty(userPermissions) && ((nodeUser.isSuperAdmin()));
    }


    public boolean isAlertVisible() {
        return ObjectUtils.isNotEmpty(nodeUser) && (nodeUser.isSuperAdmin() || roleOnThesaurusBean.isAdminOnThisThesaurus()) && nodeUser.isActive();
    }
    
    private void showErrorMessage(String msg) {
        // utilisateur ou mot de passe n'existent pas
        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_WARN, "Login Error!", msg);
        FacesContext.getCurrentInstance().addMessage(null, facesMessage);
    }

    /**
     * Permet de mettre à jour toutes les informations concernant un user #MR
     */
    private void setInfos() throws IOException {
        username = "";
        password = "";
        if (nodeUser == null) {
            return;
        }
        roleOnThesaurusBean.showListThesaurus(this, selectedTheso.getCurrentIdTheso());
        initAllAuthorizedProjectAsAdmin();

        /// Permet de vérifier après une connexion, si le thésaurus actuel est dans la liste des thésaurus authorisés pour modification
        // sinon, on nettoie l'interface et le thésaurus. 
        roleOnThesaurusBean.redirectAndCleanThesaurus(selectedTheso);
    }

    /**
     * permet de remettre à jour les informations d'un utilisateur
     */
    public void reGetUser() {
        if (nodeUser == null) {
            return;
        }
        nodeUser = userService.getUser(nodeUser.getIdUser());
    }

    public String formatUserName(String userName) {
        if (StringUtils.isEmpty(userName)) {
            return "";
        }
        return StringUtils.upperCase(userName.charAt(0) + "") + userName.substring(1);
    }

    /**
     * Permet de savoir si l'utilisateur est admin au moins sur un projet
     */
    private void initAllAuthorizedProjectAsAdmin() {
        var allAuthorizedProjectAsAdminTemp = userRoleGroupService.getRoleProjectByUser(nodeUser.getIdUser());
        if (allAuthorizedProjectAsAdmin == null) {
            allAuthorizedProjectAsAdmin = new ArrayList<>();
        } else {
            allAuthorizedProjectAsAdmin.clear();
        }
        for (NodeUserRoleGroup nodeUserRoleGroup : allAuthorizedProjectAsAdminTemp) {
            if (nodeUserRoleGroup.isAdmin()) {
                allAuthorizedProjectAsAdmin.add(nodeUserRoleGroup);
            }
        }
    }
    
}
