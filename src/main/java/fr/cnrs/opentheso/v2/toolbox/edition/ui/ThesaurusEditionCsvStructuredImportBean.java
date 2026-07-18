package fr.cnrs.opentheso.v2.toolbox.edition.ui;

import fr.cnrs.opentheso.models.nodes.NodeTree;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvStructuredImportService;
import fr.cnrs.opentheso.v2.toolbox.edition.support.CsvDelimiterSupport;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.ui.EditionBean;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.PhaseId;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusEditionCsvStructuredImportBean")
@RequiredArgsConstructor
public class ThesaurusEditionCsvStructuredImportBean implements Serializable {

    private final ThesaurusEditionCsvStructuredImportService thesaurusEditionCsvStructuredImportService;
    private final NewThesaurusService newThesaurusService;
    private final UserSession userSession;

    private String thesaurusName;
    private String selectedLang;
    private int choiceDelimiter;
    private String selectedProjectId;
    private boolean loadDone;
    private int total;
    private boolean superAdmin;

    private List<LanguageOption> allLangs = Collections.emptyList();
    private List<ProjectOption> projects = Collections.emptyList();
    private NodeTree root;

    public void init() {
        if (!ToolboxAccessPolicy.canCreateOrImportThesaurus(userSession)) {
            return;
        }
        resetForm();
        var options = newThesaurusService.loadFormOptions(
                userSession.getCurrentUserId(),
                userSession.isSuperAdmin()
        );
        allLangs = options.languages();
        projects = options.projects();
        superAdmin = options.superAdmin();
        selectedLang = allLangs.isEmpty() ? "fr" : allLangs.get(0).code();
        if (!superAdmin && projects.size() == 1) {
            selectedProjectId = String.valueOf(projects.get(0).id());
        }
    }

    public void loadFileCsvStructured(FileUploadEvent event) {
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
            return;
        }

        try (var inputStream = event.getFile().getInputStream()) {
            byte[] content = inputStream.readAllBytes();
            var result = thesaurusEditionCsvStructuredImportService.loadCsvFile(
                    content,
                    CsvDelimiterSupport.resolveDelimiter(choiceDelimiter)
            );
            if (!result.success()) {
                MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(result.error(), "Lecture CSV structuré impossible"));
                loadDone = false;
                return;
            }
            root = result.root();
            total = result.totalConcepts();
            loadDone = true;
        } catch (Exception ex) {
            loadDone = false;
            MessageUtils.showErrorMessage(ex.getMessage());
        } finally {
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    public void importThesaurus() {
        if (root == null) {
            MessageUtils.showErrorMessage("Aucun fichier CSV structuré chargé");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Utilisateur invalide");
            return;
        }

        try {
            Integer projectId = StringUtils.isBlank(selectedProjectId) ? null : Integer.parseInt(selectedProjectId);
            var outcome = thesaurusEditionCsvStructuredImportService.importNewThesaurus(
                    thesaurusName,
                    selectedLang,
                    userId,
                    userSession.getCurrentUsername(),
                    superAdmin,
                    projectId,
                    root
            );

            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Le thésaurus " + outcome.thesaurusId() + " est correctement importé !",
                    "Total importé : " + outcome.importedConcepts()
            ));
            editionBean().showList();
            PrimeFaces.current().ajax().update("messageIndex", "containerIndex");
        } catch (NumberFormatException ex) {
            MessageUtils.showErrorMessage("Projet invalide");
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Erreur pendant l'import CSV structuré"));
        } finally {
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    private void resetForm() {
        thesaurusName = "";
        choiceDelimiter = 0;
        selectedProjectId = null;
        loadDone = false;
        total = 0;
        root = null;
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{v2EditionBean}", EditionBean.class);
    }
}
