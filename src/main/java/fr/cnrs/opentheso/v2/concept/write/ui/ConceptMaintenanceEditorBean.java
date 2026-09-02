package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
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

    private final RestoreThesaurusService restoreThesaurusService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;

    private String conceptId = "";
    private String conceptLabel = "";
    private int branchCount;
    private int loopCount;
    private int repairedCount;
    private String runState = "";
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isMaintenanceActionsAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession)
                && conceptSelectionContext.hasSelection();
    }

    public boolean isLoopEmpty() {
        return loopCount <= 0;
    }

    public boolean isRepairReady() {
        return loopCount > 0 && !"done".equals(runState);
    }

    public void prepareRepairLoopedRelationships() {
        errorMessage = null;
        runState = "";
        repairedCount = 0;
        flashMessage = null;
        flashToken = null;
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
            errorMessage = "Action non autorisée";
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            errorMessage = "Erreur manque de paramètres";
            return;
        }
        var preview = restoreThesaurusService.previewLoopRelations(thesaurusId, conceptId);
        branchCount = preview.branchSize();
        loopCount = preview.loopCount();
    }

    public boolean submitRepairLoopedRelationships() {
        errorMessage = null;
        if (!isMaintenanceActionsAvailable()) {
            runState = "error";
            errorMessage = "Action non autorisée";
            return false;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String id = StringUtils.defaultIfBlank(conceptId, conceptSelectionContext.getConceptId());
        if (StringUtils.isAnyBlank(thesaurusId, id)) {
            runState = "error";
            errorMessage = "Erreur manque de paramètres";
            return false;
        }
        try {
            repairedCount = restoreThesaurusService.deleteLoopRelations(thesaurusId, id);
            var preview = restoreThesaurusService.previewLoopRelations(thesaurusId, id);
            branchCount = preview.branchSize();
            loopCount = preview.loopCount();
            runState = "done";
            flashSuccess(repairedCount <= 0
                    ? "Aucune relation en boucle à corriger"
                    : repairedCount == 1
                    ? "1 relation en boucle supprimée"
                    : repairedCount + " relations en boucle supprimées");
            return true;
        } catch (Exception e) {
            runState = "error";
            errorMessage = StringUtils.defaultIfBlank(e.getMessage(), "La réparation a échoué");
            return false;
        }
    }

    private void flashSuccess(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
    }
}
