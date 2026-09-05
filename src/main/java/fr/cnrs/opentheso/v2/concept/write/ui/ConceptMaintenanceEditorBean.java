package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Actions de maintenance concept (alignées sur le menu fil d'Ariane legacy).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptMaintenanceEditorBean")
@RequiredArgsConstructor
public class ConceptMaintenanceEditorBean implements Serializable {

    private final transient RestoreThesaurusService restoreThesaurusService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient V2LocaleBean v2LocaleBean;

    private final DialogRunState run = new DialogRunState();

    private String conceptId = "";
    private String conceptLabel = "";
    private int branchCount;
    private int loopCount;
    private int repairedCount;

    public String getRunState() {
        return run.getState();
    }

    public void setRunState(String state) {
        run.setState(state);
    }

    public String getErrorMessage() {
        return run.getErrorMessage();
    }

    public void setErrorMessage(String errorMessage) {
        run.setErrorMessage(errorMessage);
    }

    public String getFlashMessage() {
        return run.getFlashMessage();
    }

    public void setFlashMessage(String flashMessage) {
        run.setFlashMessage(flashMessage);
    }

    public String getFlashToken() {
        return run.getFlashToken();
    }

    public void setFlashToken(String flashToken) {
        run.setFlashToken(flashToken);
    }

    public boolean isMaintenanceActionsAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession)
                && conceptSelectionContext.hasSelection();
    }

    public boolean isLoopEmpty() {
        return loopCount <= 0;
    }

    public boolean isRepairReady() {
        return loopCount > 0 && !run.isDone();
    }

    public void prepareRepairLoopedRelationships() {
        run.reset();
        repairedCount = 0;
        conceptId = conceptSelectionContext.hasSelection()
                ? StringUtils.defaultString(conceptSelectionContext.getConceptId())
                : "";
        conceptLabel = conceptSelectionContext.hasSelection()
                && conceptSelectionContext.getSummary() != null
                ? StringUtils.defaultString(conceptSelectionContext.getSummary().preferredLabel())
                : "";
        branchCount = 0;
        loopCount = 0;
        if (!isMaintenanceActionsAvailable()) {
            run.setErrorMessage(unauthorized());
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            run.setErrorMessage(msg("v2.write.missingParams", "Erreur manque de paramètres"));
            return;
        }
        var preview = restoreThesaurusService.previewLoopRelations(thesaurusId, conceptId);
        branchCount = preview.branchSize();
        loopCount = preview.loopCount();
    }

    public boolean submitRepairLoopedRelationships() {
        run.setErrorMessage(null);
        if (!isMaintenanceActionsAvailable()) {
            run.fail(unauthorized());
            return false;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String id = StringUtils.defaultIfBlank(conceptId, conceptSelectionContext.getConceptId());
        if (StringUtils.isAnyBlank(thesaurusId, id)) {
            run.fail(msg("v2.write.missingParams", "Erreur manque de paramètres"));
            return false;
        }
        try {
            repairedCount = restoreThesaurusService.deleteLoopRelations(thesaurusId, id);
            var preview = restoreThesaurusService.previewLoopRelations(thesaurusId, id);
            branchCount = preview.branchSize();
            loopCount = preview.loopCount();
            run.succeed(repairedCount <= 0
                    ? msg("v2.concept.loopNone", "Aucune relation en boucle à corriger")
                    : repairedCount == 1
                    ? msg("v2.concept.loopFixedOne", "1 relation en boucle supprimée")
                    : msg("v2.concept.loopFixedMany", "{0} relations en boucle supprimées", repairedCount));
            return true;
        } catch (Exception e) {
            run.fail(StringUtils.defaultIfBlank(e.getMessage(), msg("v2.concept.loopFailed", "La réparation a échoué")));
            return false;
        }
    }

    public void finishAfterClose() {
        run.reset();
    }


    private String unauthorized() {
        return WriteUiMessages.unauthorized(v2LocaleBean);
    }

    private String msg(String key, String fallback) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback);
    }

    private String msg(String key, String fallback, Object... args) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback, args);
    }
}
