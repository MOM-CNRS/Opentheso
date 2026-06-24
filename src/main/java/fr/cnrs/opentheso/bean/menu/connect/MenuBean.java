package fr.cnrs.opentheso.bean.menu.connect;

import fr.cnrs.opentheso.bean.candidat.CandidatBean;
import fr.cnrs.opentheso.bean.graph.DataGraphView;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.bean.profile.MyAccountBean;
import fr.cnrs.opentheso.bean.profile.MyProjectBean;
import fr.cnrs.opentheso.bean.profile.SuperAdminBean;
import fr.cnrs.opentheso.bean.proposition.PropositionBean;
import fr.cnrs.opentheso.bean.setting.CorpusBean;
import fr.cnrs.opentheso.bean.setting.PreferenceBean;
import fr.cnrs.opentheso.bean.toolbox.atelier.AtelierThesBean;
import fr.cnrs.opentheso.bean.toolbox.edition.FlagBean;
import fr.cnrs.opentheso.bean.toolbox.edition.ViewEditionBean;
import fr.cnrs.opentheso.bean.toolbox.statistique.StatistiqueBean;

import java.io.Serializable;
import java.io.IOException;
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "menuBean")
public class MenuBean implements Serializable {

    @Autowired @Lazy
    private AtelierThesBean atelierThesBean;

    private final SuperAdminBean superAdminBean;
    private final MyAccountBean myAccountBean;
    private final MyProjectBean myProjectBean;
    private final CorpusBean corpusBean;
    private final PreferenceBean preferenceBean;
    private final ViewEditionBean viewEditionBean;
    private final FlagBean flagBean;
    private final StatistiqueBean statistiqueBean;
    private final SelectedTheso selectedTheso;
    private final CandidatBean candidatBean;
    private final CurrentUser currentUser;
    private final PropositionBean propositionBean;
    private final DataGraphView dataGraphView;

    private boolean notificationPanelVisible;
    private String activePageName = "index";
    
    
    
    public boolean checkIfUserIsConnected() throws IOException {
        if (currentUser.getNodeUser() == null) {
            redirectToThesaurus();
            return false;
        }
        return true;
    }
    
    // LOGIN Page
    public void redirectToThesaurus() throws IOException {
        activePageName = "index";
        notificationPanelVisible = true;
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/index.xhtml");
    }
    
    // LOGIN Page
    public void redirectToCandidatPage() throws IOException {
        activePageName = "candidat";
        notificationPanelVisible = false;
        candidatBean.initCandidatModule();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/candidat/candidat.xhtml");
    }
    
    // LOGIN Page
    public void redirectToGraphPage() throws IOException {
        activePageName = "graph";
        notificationPanelVisible = false;
        dataGraphView.init();
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/graphview/graph.xhtml");
    }    
    
    // MENU Profile
    public void redirectToUsersPage() throws IOException {
        activePageName = "users";
        notificationPanelVisible = false;
        superAdminBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/profile/users.xhtml");
    }

    public void redirectToProjetsPage() throws IOException {
        activePageName = "Projects";
        notificationPanelVisible = false;
        superAdminBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/profile/projects.xhtml");
    }

    public void redirectToThesorusPage() throws IOException {
        activePageName = "thesorus";
        notificationPanelVisible = false;
        superAdminBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/profile/thesaurus.xhtml");
    }

    public void redirectToUsersV2Page() throws IOException {
        activePageName = "usersV2";
        notificationPanelVisible = false;
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/v2/admin/all-users.xhtml");
    }

    public void redirectToProjectsV2Page() throws IOException {
        activePageName = "projectsV2";
        notificationPanelVisible = false;
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/v2/admin/all-projects.xhtml");
    }

    public void redirectToThesauriV2Page() throws IOException {
        activePageName = "thesauriV2";
        notificationPanelVisible = false;
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/v2/admin/all-thesauri.xhtml");
    }
    
    public void redirectToMyProfilePage() throws IOException {
        activePageName = "myAccount";
        notificationPanelVisible = false;
        myAccountBean.loadDataPage();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/profile/myAccount.xhtml");
    }

    public void redirectToMyProfileV2Page() throws IOException {
        activePageName = "myAccountV2";
        notificationPanelVisible = false;
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/v2/user/my-account.xhtml");
    }

    public void redirectToMesProjectsV2Page() throws IOException {
        activePageName = "myProjectV2";
        notificationPanelVisible = false;
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/v2/project/my-projects.xhtml");
    }
    
    public void redirectToMesProjectsPage() throws IOException {
        activePageName = "myProject";
        notificationPanelVisible = false;
        myProjectBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/profile/myProject.xhtml");
    }
    
    // MENU Paramètres
    public void redirectToIdetifiantPage() throws IOException {
        activePageName = "identifier";
        notificationPanelVisible = false;
        preferenceBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/setting/identifier.xhtml");
    }
    
    public void redirectToPreferencePage() throws IOException {
        activePageName = "preference";
        notificationPanelVisible = false;
        preferenceBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/setting/preference.xhtml");
    }
    
    public void redirectToCorpusPage() throws IOException {
        activePageName = "corpus";
        notificationPanelVisible = false;
        corpusBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/setting/corpus.xhtml");
    }
    
    ///Boite à outils
    public void redirectToEditionPage() throws IOException {
        activePageName = "edition";
        notificationPanelVisible = false;
        viewEditionBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/toolbox/edition.xhtml");
    }

    public void redirectToFlagPage() throws IOException {
        activePageName = "flag";
        notificationPanelVisible = false;
        flagBean.init();
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/toolbox/flag.xhtml");
    }
    
    public void redirectToAtelierPage() throws IOException {
        activePageName = "atelier";
        atelierThesBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/toolbox/atelier.xhtml");
    }
    
    public void redirectToMaintenancePage() throws IOException {
        activePageName = "service";
        notificationPanelVisible = false;
        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "", 
                    "Vous devez choisir un Thesorus avant !"));
            PrimeFaces pf = PrimeFaces.current();
            pf.ajax().update("messageIndex");
            return;
        }
        atelierThesBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/toolbox/service.xhtml");
    }
    
    public void redirectToStatistiquePage() throws IOException {
        activePageName = "statistic";
        notificationPanelVisible = false;
        if (StringUtils.isEmpty(selectedTheso.getCurrentIdTheso())) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "", 
                    "Vous devez choisir un Thesorus avant !"));
            PrimeFaces pf = PrimeFaces.current();
            pf.ajax().update("messageIndex");
            return;
        }
        statistiqueBean.init();
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/toolbox/statistic.xhtml");
    }

    // LOGIN Page
    public void redirectToLoginPage() throws IOException {
        activePageName = "login";
        notificationPanelVisible = false;
        propositionBean.searchNewPropositions();
        propositionBean.setRubriqueVisible(false);
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        context.redirect(context.getRequestContextPath() + "/login.xhtml");
    }
}
