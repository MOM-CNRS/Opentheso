package fr.cnrs.opentheso.v2.toolbox.edition.ui;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.persistence.V2ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.toolbox.model.LanguageOption;
import fr.cnrs.opentheso.v2.toolbox.model.ProjectOption;
import fr.cnrs.opentheso.v2.toolbox.policy.ToolboxAccessPolicy;
import fr.cnrs.opentheso.v2.toolbox.service.NewThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.edition.service.ThesaurusEditionSkosImportService;
import fr.cnrs.opentheso.v2.toolbox.ui.EditionBean;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ThesaurusEditionSkosImportBean")
@RequiredArgsConstructor
public class ThesaurusEditionSkosImportBean implements Serializable {

    private final transient ThesaurusEditionSkosImportService thesaurusEditionSkosImportService;
    private final transient NewThesaurusService newThesaurusService;
    private final transient UserSession userSession;
    private final transient ToolboxAccessPolicy toolboxAccessPolicy;
    private final transient V2ThesaurusPreferencesProvider v2ThesaurusPreferencesProvider;

    private int typeImport;
    private int total;
    private String uri;
    private String selectedLang;
    private String persistentNameThesaurus;
    private String formatDate = "yyyy-MM-dd";
    private String selectedIdentifier = "sans";
    private String prefixHandle = "";
    private String prefixDoi = "";
    private String selectedProjectId;
    private boolean loadDone;
    private int progressValue;
    /** true si un thésaurus du même titre existe déjà → l'utilisateur peut choisir maître/esclave. */
    private boolean existingThesaurusDetected;
    private String existingThesaurusId;
    /** true = maître ; false = esclave (défaut). */
    private boolean importAsMaster;

    private List<LanguageOption> allLangs = Collections.emptyList();
    private List<ProjectOption> projects = Collections.emptyList();
    private boolean superAdmin;
    private SKOSXmlDocument skosXmlDocument;

    public void init() {
        if (!toolboxAccessPolicy.canCreateOrImportThesaurus(userSession)) {
            return;
        }
        typeImport = 0;
        total = 0;
        uri = "";
        loadDone = false;
        progressValue = 0;
        skosXmlDocument = null;
        formatDate = "yyyy-MM-dd";
        selectedIdentifier = "sans";
        prefixHandle = "";
        prefixDoi = "";
        selectedProjectId = null;
        existingThesaurusDetected = false;
        existingThesaurusId = null;
        importAsMaster = false;

        var options = newThesaurusService.loadFormOptions(
                userSession.getCurrentUserId(),
                userSession.isSuperAdmin()
        );
        allLangs = options.languages();
        projects = options.projects();
        superAdmin = options.superAdmin();
        // fr si présent dans la liste, sinon première langue disponible
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

    public void stateChangeListener(AjaxBehaviorEvent event) {
        // Conservé pour compatibilité avec le composant JSF.
    }

    public void loadFileSkos(FileUploadEvent event) {
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
            return;
        }

        var error = new StringBuilder();
        existingThesaurusDetected = false;
        existingThesaurusId = null;
        importAsMaster = false;
        try (InputStream inputStream = event.getFile().getInputStream()) {
            if (StringUtils.isBlank(selectedLang)) {
                selectedLang = resolveDefaultSourceLang();
            }
            var result = thesaurusEditionSkosImportService.loadSkosFile(inputStream, typeImport, selectedLang, error);
            skosXmlDocument = result.document();
            total = result.totalConcepts();
            uri = result.uri();
            loadDone = true;
            detectExistingThesaurus();
        } catch (Exception ex) {
            loadDone = true;
            error.append(ex.getMessage());
        }

        if (!error.isEmpty()) {
            MessageUtils.showErrorMessage(error.toString());
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
    }

    private void detectExistingThesaurus() {
        existingThesaurusDetected = false;
        existingThesaurusId = null;
        importAsMaster = false;
        if (skosXmlDocument == null) {
            return;
        }
        Integer projectId = null;
        if (StringUtils.isNotBlank(selectedProjectId)) {
            try {
                projectId = Integer.parseInt(selectedProjectId);
            } catch (NumberFormatException ignored) {
                projectId = null;
            }
        }
        var existing = thesaurusEditionSkosImportService.findExistingThesaurusId(
                skosXmlDocument, projectId, selectedLang);
        if (existing.isPresent()) {
            existingThesaurusDetected = true;
            existingThesaurusId = existing.get();
        }
    }

    public void onProjectChanged() {
        if (loadDone && skosXmlDocument != null) {
            detectExistingThesaurus();
        }
    }

    public void importThesaurus() {
        if (skosXmlDocument == null) {
            MessageUtils.showErrorMessage("Aucun fichier SKOS chargé");
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
            boolean asMaster = existingThesaurusDetected && importAsMaster;
            String thesaurusId = thesaurusEditionSkosImportService.importNewThesaurus(
                    skosXmlDocument,
                    formatDate,
                    userId,
                    superAdmin,
                    projectId,
                    selectedLang,
                    selectedIdentifier,
                    prefixHandle,
                    prefixDoi,
                    persistentNameThesaurus,
                    asMaster
            );
            progressValue = 100;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Le thésaurus " + thesaurusId + " est correctement ajouté !",
                    "import réussi"
            ));
            editionBean().showList();
            PrimeFaces.current().ajax().update("messageIndex", "containerIndex");
        } catch (NumberFormatException ex) {
            MessageUtils.showErrorMessage("Projet invalide");
        } catch (Exception ex) {
            MessageUtils.showErrorMessage(StringUtils.defaultIfBlank(ex.getMessage(), "Erreur pendant l'import SKOS"));
        } finally {
            PrimeFaces.current().executeScript("PF('waitDialog').hide();");
        }
    }

    private EditionBean editionBean() {
        FacesContext context = FacesContext.getCurrentInstance();
        return context.getApplication().evaluateExpressionGet(context, "#{v2EditionBean}", EditionBean.class);
    }
}
