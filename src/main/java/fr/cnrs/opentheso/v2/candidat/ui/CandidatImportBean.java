package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.model.CandidatImportLanguage;
import fr.cnrs.opentheso.v2.candidat.service.CandidatLanguageService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatSkosImportService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2CandidatImportBean")
@RequiredArgsConstructor
public class CandidatImportBean implements Serializable {

    private final CandidatSkosImportService candidatSkosImportService;
    private final CandidatLanguageService candidatLanguageService;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final CandidatBean candidatBean;

    private int typeImport;
    private int total;
    private String uri;
    private String selectedLang;
    private boolean loadDone;

    private List<CandidatImportLanguage> allLangs;
    private SKOSXmlDocument skosXmlDocument;

    public void init() {
        thesaurusContext.syncFromViewParams();
        typeImport = 0;
        total = 0;
        uri = "";
        loadDone = false;
        skosXmlDocument = null;
        allLangs = candidatLanguageService.listAllLanguages();
        selectedLang = thesaurusContext.resolveWorkLanguage();
    }

    public void stateChangeListener(AjaxBehaviorEvent event) {
        // Conservé pour compatibilité avec le composant JSF legacy.
    }

    public void loadFileSkos(FileUploadEvent event) {
        if (!PhaseId.INVOKE_APPLICATION.equals(event.getPhaseId())) {
            event.setPhaseId(PhaseId.INVOKE_APPLICATION);
            event.queue();
            return;
        }

        var error = new StringBuffer();
        try (InputStream inputStream = event.getFile().getInputStream()) {
            var result = candidatSkosImportService.loadSkosFile(inputStream, typeImport, selectedLang, error);
            skosXmlDocument = result.document();
            total = result.totalConcepts();
            uri = result.uri();
            loadDone = true;
        } catch (Exception ex) {
            loadDone = true;
            error.append(ex.getMessage());
        }

        if (!error.isEmpty()) {
            MessageUtils.showErrorMessage(error.toString());
        }
        PrimeFaces.current().executeScript("PF('waitDialog').hide();");
    }

    public void addSkosCandidatToBDD() {
        if (skosXmlDocument == null || skosXmlDocument.getConceptList() == null) {
            MessageUtils.showErrorMessage("Aucun fichier candidat chargé");
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        Integer userId = userSession.getCurrentUserId();
        if (thesaurusId == null || thesaurusId.isBlank() || userId == null) {
            MessageUtils.showErrorMessage("Contexte thésaurus ou utilisateur invalide");
            return;
        }

        try {
            var concepts = skosXmlDocument.getConceptList();
            candidatBean.prepareImportProgress(concepts.size());
            candidatSkosImportService.importCandidatesForThesaurus(
                    skosXmlDocument,
                    thesaurusId,
                    userId,
                    thesaurusContext.resolveWorkLanguage(),
                    candidatBean::updateImportProgress
            );
            candidatBean.setListCandidatsActivate(true);
            onComplete();
        } catch (IOException ex) {
            MessageUtils.showErrorMessage("Erreur pendant l'import des candidats");
        }
    }

    private void onComplete() {
        candidatBean.setProgressBarValue(100);
        showSuccess("import réussi");
        PrimeFaces.current().executeScript("PF('pbAjax').cancel();");
    }

    private void showSuccess(String message) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Info :", message));
        PrimeFaces.current().ajax().update("messageIndex");
    }
}
