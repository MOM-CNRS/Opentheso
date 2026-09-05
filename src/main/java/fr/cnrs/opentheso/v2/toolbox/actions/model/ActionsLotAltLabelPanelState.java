package fr.cnrs.opentheso.v2.toolbox.actions.model;

/**
 * État UI d'un panneau Importer / Supprimer (formes alternatives).
 */
public class ActionsLotAltLabelPanelState extends ActionsLotImportPanelState<ActionsLotAltLabelCandidate> {

    public void applyValidation(ActionsLotAltLabelValidationResult result) {
        super.applyValidation(new ActionsLotImportValidationResult<>(
                result.success(),
                result.errorMessage(),
                result.linesRead(),
                result.validCount(),
                result.errorCount(),
                result.ignoredCount(),
                result.errors(),
                result.validCandidates()
        ));
    }
}
