package fr.cnrs.opentheso.v2.toolbox.edition.ui;

import fr.cnrs.opentheso.v2.toolbox.edition.model.ThesaurusCsvConceptObject;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionCsvImportService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusEditionCsvImportBean")
@RequiredArgsConstructor
public class ThesaurusEditionCsvImportBean implements Serializable {

    private final transient ThesaurusEditionCsvImportService thesaurusEditionCsvImportService;
    private final transient NewThesaurusService newThesaurusService;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;

    private String thesaurusName;
    private String selectedLang;
    private String persistentNameThesaurus;
    private String formatDate = "yyyy-MM-dd";
    private int choiceDelimiter;
    private String selectedProjectId;
    private boolean loadDone;
    private int total;
    private String warning;

    private List<LanguageOption> allLangs = Collections.emptyList();
    private List<ProjectOption> projects = Collections.emptyList();
    private List<String> detectedLangs = Collections.emptyList();
    private boolean superAdmin;

    private List<ThesaurusCsvConceptObject> conceptObjects = Collections.emptyList();

    public void init() {
        if (!toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)) {
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
        selectedLang = resolveDefaultSourceLang();
        if (!superAdmin && projects.size() == 1) {
            selectedProjectId = String.valueOf(projects.get(0).id());
        }
    }

    private String resolveDefaultSourceLang() {
        if (allLangs == null || allLangs.isEmpty()) {
            return "fr";
        }
        return allLangs.stream()
                .map(LanguageOption::code)
                .filter(code -> "fr".equalsIgnoreCase(code))
                .findFirst()
                .orElse(allLangs.get(0).code());
    }

    public void loadFileCsv(FileUploadEvent event) {
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
            return;
        }

        try (var inputStream = event.getFile().getInputStream()) {
            byte[] content = inputStream.readAllBytes();
            var result = thesaurusEditionCsvImportService.loadCsvFile(
                    content,
                    CsvDelimiterSupport.resolveDelimiter(choiceDelimiter)
            );
            if (!result.success()) {
                MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(result.error(), "Lecture CSV impossible"));
                loadDone = false;
                return;
            }
            conceptObjects = new ArrayList<>(result.conceptObjects());
            detectedLangs = result.languages();
            total = result.totalConcepts();
            warning = result.warning();
            loadDone = true;
        } catch (Exception ex) {
            loadDone = false;
            MessageUtils.showErrorMessage(ex.getMessage());
        } finally {
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    public void importThesaurus() {
        if (conceptObjects == null || conceptObjects.isEmpty()) {
            MessageUtils.showErrorMessage("Aucun fichier CSV chargé");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Utilisateur invalide");
            return;
        }

        try {
            if (StringUtils.isBlank(selectedLang)) {
                selectedLang = resolveDefaultSourceLang();
            }
            Integer projectId = StringUtils.isBlank(selectedProjectId) ? null : Integer.parseInt(selectedProjectId);
            var outcome = thesaurusEditionCsvImportService.importNewThesaurus(
                    thesaurusName,
                    selectedLang,
                    formatDate,
                    userId,
                    userSession.getCurrentUsername(),
                    superAdmin,
                    projectId,
                    conceptObjects,
                    detectedLangs,
                    persistentNameThesaurus
            );

            String detail = "Total importé : " + outcome.importedConcepts();
            if (StringUtils.isNotBlank(outcome.message())) {
                MessageUtils.showWarnMessage(outcome.message() + ", " + detail);
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Le thésaurus " + outcome.thesaurusId() + " est correctement ajouté !",
                        detail
                ));
            }
            editionBean().showList();
            PrimeFaces.current().ajax().update("messageIndex", "containerIndex");
        } catch (NumberFormatException ex) {
            MessageUtils.showErrorMessage("Projet invalide");
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Erreur pendant l'import CSV"));
        } finally {
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    private void resetForm() {
        thesaurusName = "";
        formatDate = "yyyy-MM-dd";
        choiceDelimiter = 0;
        selectedProjectId = null;
        loadDone = false;
        total = 0;
        warning = null;
        conceptObjects = Collections.emptyList();
        detectedLangs = Collections.emptyList();
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{v2EditionBean}", EditionBean.class);
    }
}
