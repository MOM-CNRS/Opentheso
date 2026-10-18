package fr.cnrs.opentheso.toolbox.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.concept.ui.ConsultationShellBean;
import fr.cnrs.opentheso.shared.ui.UserSession;
import fr.cnrs.opentheso.toolbox.exception.InvalidToolboxDataException;
import fr.cnrs.opentheso.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.toolbox.service.NewThesaurusService;
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
@Named("newThesaurusBean")
public class NewThesaurusBean implements Serializable {

    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;
    private final transient NewThesaurusService newThesaurusService;

    private NewThesaurusEditor editor = NewThesaurusEditor.empty();
    private List<LanguageOption> languages = Collections.emptyList();
    private List<ProjectOption> projects = Collections.emptyList();
    private boolean superAdmin;

    public NewThesaurusBean(
            UserSession userSession,
            ToolboxAccessPolicy toolboxAccessPolicy,
            NewThesaurusService newThesaurusService
    ) {
        this.userSession = userSession;
        this.toolboxAccessPolicy = toolboxAccessPolicy;
        this.newThesaurusService = newThesaurusService;
    }

    public boolean isFormAvailable() {
        return toolboxAccessPolicy.canCreateOrImportThesaurus(userSession);
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
            newThesaurusService.create(
                    editor.toRequest(),
                    userSession.getCurrentUsername(),
                    userSession.getCurrentUserId()
            );
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
        FacesContext context = FacesContext.getCurrentInstance();
        ConsultationShellBean shellBean = context.getApplication().evaluateExpressionGet(
                context, "#{consultationShellBean}", ConsultationShellBean.class);
        shellBean.refreshHeaderCatalog();
        PrimeFaces.current().ajax().update("containerIndex:header");
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{editionBean}", EditionBean.class);
    }
}
