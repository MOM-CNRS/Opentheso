package fr.cnrs.opentheso.v2.toolbox.actions.model;

/**
 * État UI d'un panneau Importer / Supprimer (alignements).
 */
public class ActionsLotPanelState extends ActionsLotImportPanelState<ActionsLotAlignmentCandidate> {

    public void applyValidation(ActionsLotValidationResult result) {
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
