package fr.cnrs.opentheso.bean.toolbox.edition;

import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.entites.UserGroupLabel;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.services.EditThesaurusService;
import fr.cnrs.opentheso.services.PreferenceService;
import fr.cnrs.opentheso.services.ProjectService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;

import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;


@Getter
@Setter
@SessionScoped
@RequiredArgsConstructor
@Named(value = "newThesaurusBean")
public class NewThesaurusBean implements Serializable {

    private final CurrentUser currentUser;
    private final RoleOnThesaurusBean roleOnThesoBean;
    private final ViewEditionBean viewEditionBean;
    private final SelectedTheso selectedTheso;
    private final PreferenceService preferenceService;
    private final LanguageRepository languageRepository;
    private final ThesaurusService thesaurusService;
    private final EditThesaurusService editThesaurusService;
    private final ProjectService projectService;

    private List<LanguageIso639> allLangs;
    private List<UserGroupLabel> nodeProjects;
    private String title, selectedLang, selectedProject, persistentNameTheso;


    public void init() {
        
        allLangs = languageRepository.findAllOrderByCodePays();
        selectedLang = null;
        selectedProject = "";
        title = "";
        if (currentUser.getNodeUser().isSuperAdmin()) {
            nodeProjects = projectService.getAllProjects();
            nodeProjects.sort(Comparator.comparing(UserGroupLabel::getLabel, String.CASE_INSENSITIVE_ORDER));
        } else {
            nodeProjects = projectService.getProjectByUser(currentUser.getNodeUser().getIdUser(), 2);
            for (UserGroupLabel userGroupLabel : nodeProjects) {
                selectedProject = "" + userGroupLabel.getId();
            }
        }
    }

    /**
     * Permet de supprimer un thésaurus
     */
    public void addNewThesaurus() {

        if (StringUtils.isEmpty(title)) {
            MessageUtils.showErrorMessage("Le label est obligatoire");
            return;
        }

        if (StringUtils.isEmpty(selectedLang)) {
            MessageUtils.showErrorMessage("La langue est obligatoire");
            return;
        }

        String idTheso = editThesaurusService.addNewThesaurus(title, selectedLang, selectedProject, currentUser.getNodeUser().getName());

        if(StringUtils.isNotBlank(persistentNameTheso)) {
            preferenceService.updatePersistentNameTheso(idTheso, persistentNameTheso);
        }

        MessageUtils.showInformationMessage("Thesaurus ajouté avec succès");

        init();
        roleOnThesoBean.showListThesaurus(currentUser, selectedTheso.getCurrentIdTheso());
        viewEditionBean.init();

        PrimeFaces.current().ajax().update("formMenu:idListTheso");
        PrimeFaces.current().ajax().update("toolBoxForm");
        PrimeFaces.current().ajax().update("toolBoxForm:listThesoForm");
    }

}
