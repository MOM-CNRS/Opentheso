package fr.cnrs.opentheso.v2.candidat.ui;

import fr.cnrs.opentheso.bean.menu.theso.RoleOnThesaurusBean;
import fr.cnrs.opentheso.bean.menu.theso.SelectedTheso;
import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.entites.LanguageIso639;
import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.repositories.LanguageRepository;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.service.CandidatSkosImportService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.event.PhaseId;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Named("v2CandidatImportBean")
@ViewScoped
public class CandidatImportBean implements Serializable {

    @Value("${settings.workLanguage:fr}")
    private String workLanguage;

    private final CandidatSkosImportService candidatSkosImportService;
    private final LanguageRepository languageRepository;
    private final RoleOnThesaurusBean roleOnThesaurus;
    private final SelectedTheso selectedTheso;
    private final CurrentUser currentUser;
    private final CandidatBean candidatBean;

    private int typeImport;
    private int total;
    private String uri;
    private String selectedLang;
    private boolean loadDone;

    private List<LanguageIso639> allLangs;
    private SKOSXmlDocument skosXmlDocument;

    public CandidatImportBean(
            CandidatSkosImportService candidatSkosImportService,
            LanguageRepository languageRepository,
            RoleOnThesaurusBean roleOnThesaurus,
            SelectedTheso selectedTheso,
            CurrentUser currentUser,
            CandidatBean candidatBean
    ) {
        this.candidatSkosImportService = candidatSkosImportService;
        this.languageRepository = languageRepository;
        this.roleOnThesaurus = roleOnThesaurus;
        this.selectedTheso = selectedTheso;
        this.currentUser = currentUser;
        this.candidatBean = candidatBean;
    }

    public void init() {
        typeImport = 0;
        total = 0;
        uri = "";
        loadDone = false;
        skosXmlDocument = null;
        allLangs = languageRepository.findAll();
        if (roleOnThesaurus.getNodePreference() != null) {
            selectedLang = roleOnThesaurus.getNodePreference().getSourceLang();
        } else {
            selectedLang = workLanguage;
        }
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
        if (roleOnThesaurus.getNodePreference() == null) {
            MessageUtils.showErrorMessage("Préférences du thésaurus introuvables");
            return;
        }

        try {
            var concepts = skosXmlDocument.getConceptList();
            candidatBean.prepareImportProgress(concepts.size());
            candidatSkosImportService.importCandidates(
                    skosXmlDocument,
                    selectedTheso.getCurrentIdTheso(),
                    currentUser.getNodeUser().getIdUser(),
                    -1,
                    roleOnThesaurus.getNodePreference().getSourceLang(),
                    roleOnThesaurus.getNodePreference(),
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
