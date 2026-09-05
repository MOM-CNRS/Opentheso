package fr.cnrs.opentheso.v2.concept.io.ui;

import fr.cnrs.opentheso.models.skosapi.SKOSXmlDocument;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.model.CandidatImportLanguage;
import fr.cnrs.opentheso.v2.candidat.service.CandidatLanguageService;
import fr.cnrs.opentheso.v2.concept.io.service.ConceptSkosImportService;
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
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptSkosImportBean")
@RequiredArgsConstructor
public class ConceptSkosImportBean implements Serializable {

    private final transient ConceptSkosImportService conceptSkosImportService;
    private final transient CandidatLanguageService candidatLanguageService;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;

    private int typeImport;
    private int total;
    private String uri;
    private String selectedLang;
    private boolean loadDone;
    private int progressValue;

    private List<CandidatImportLanguage> allLangs;
    private SKOSXmlDocument skosXmlDocument;

    public void init() {
        thesaurusContext.syncFromViewParams();
        typeImport = 0;
        total = 0;
        uri = "";
        loadDone = false;
        progressValue = 0;
        skosXmlDocument = null;
        allLangs = candidatLanguageService.listAllLanguages();
        selectedLang = thesaurusContext.resolveWorkLanguage();
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
        try (InputStream inputStream = event.getFile().getInputStream()) {
            var result = conceptSkosImportService.loadSkosFile(inputStream, typeImport, selectedLang, error);
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

    public void importConcepts() {
        if (skosXmlDocument == null || skosXmlDocument.getConceptList() == null) {
            MessageUtils.showErrorMessage("Aucun fichier SKOS chargé");
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        Integer userId = userSession.getCurrentUserId();
        if (StringUtils.isBlank(thesaurusId) || userId == null) {
            MessageUtils.showErrorMessage("Contexte thésaurus ou utilisateur invalide");
            return;
        }

        try {
            conceptSkosImportService.importConceptsForThesaurus(
                    skosXmlDocument,
                    thesaurusId,
                    userId,
                    thesaurusContext.resolveWorkLanguage(),
                    (current, max) -> progressValue = max == 0 ? 0 : (current * 100) / max
            );
            progressValue = 100;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Import SKOS terminé",
                    null
            ));
        } catch (IOException ex) {
            MessageUtils.showErrorMessage("Erreur pendant l'import SKOS");
        }
    }
}
