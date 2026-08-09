package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.services.RestoreThesaurusService;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;

/**
 * Actions de maintenance concept (alignées sur le menu fil d'Ariane legacy).
 */
@ViewScoped
@Named("v2ConceptMaintenanceEditorBean")
@RequiredArgsConstructor
public class ConceptMaintenanceEditorBean implements Serializable {

    private final RestoreThesaurusService restoreThesaurusService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;

    public boolean isMaintenanceActionsAvailable() {
        return conceptWritePolicy.canMutateConcept(userSession);
    }

    public void repairLoopedRelationships() {
        if (!isMaintenanceActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String conceptId = conceptSelectionContext.getConceptId();
        if (StringUtils.isAnyBlank(thesaurusId, conceptId)) {
            MessageUtils.showErrorMessage("Erreur manque de paramètres");
            return;
        }
        restoreThesaurusService.deleteLoopRelations(thesaurusId, conceptId);
        conceptNavigationSupport.refreshSelectedConcept();
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :containerIndex:formLeftTab :messageIndex");
        MessageUtils.showInformationMessage("Correction terminée");
    }
}
