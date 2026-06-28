package fr.cnrs.opentheso.v2.toolbox.ui;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@ViewScoped
@Named("v2NewThesaurusBean")
public class NewThesaurusBean implements Serializable {

    private final UserSession userSession;
    private final CurrentUser currentUser;
    private final NewThesaurusService newThesaurusService;

    private NewThesaurusEditor editor = NewThesaurusEditor.empty();
    private List<LanguageOption> languages = Collections.emptyList();
    private List<ProjectOption> projects = Collections.emptyList();
    private boolean superAdmin;

    public NewThesaurusBean(
            UserSession userSession,
            CurrentUser currentUser,
            NewThesaurusService newThesaurusService
    ) {
        this.userSession = userSession;
        this.currentUser = currentUser;
        this.newThesaurusService = newThesaurusService;
    }

    public boolean isFormAvailable() {
        return ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession);
    }

    public void prepareForm() {
        if (!isFormAvailable()) {
            editor = NewThesaurusEditor.empty();
            languages = Collections.emptyList();
            projects = Collections.emptyList();
            return;
        }
        var options = newThesaurusService.loadFormOptions(
                userSession.getCurrentUserId(),
                userSession.isSuperAdmin()
        );
        editor = NewThesaurusEditor.empty();
        languages = options.languages();
        projects = options.projects();
        superAdmin = options.superAdmin();
        if (!superAdmin && projects.size() == 1) {
            editor.setSelectedProjectId(String.valueOf(projects.get(0).id()));
        }
    }

    public void create() {
        if (!isFormAvailable()) {
            return;
        }
        try {
            newThesaurusService.create(editor.toRequest(), userSession.getCurrentUsername());
            MessageUtils.showInformationMessage("Thesaurus ajouté avec succès");
            refreshMenuThesaurusList();
            editionBean().showList();
        } catch (InvalidToolboxDataException e) {
            MessageUtils.showErrorMessage(e.getMessage());
        }
    }

    public void cancel() {
        editor = NewThesaurusEditor.empty();
        editionBean().showList();
    }

    private void refreshMenuThesaurusList() {
        PrimeFaces.current().ajax().update("formMenu:idListTheso");
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{v2EditionBean}", EditionBean.class);
    }
}
