package fr.cnrs.opentheso.v2.toolbox.actions.model;

/**
 * État UI du panneau Importer (notes).
 */
public class ActionsLotNotePanelState extends ActionsLotImportPanelState<ActionsLotNoteCandidate> {

    public void applyValidation(ActionsLotNoteValidationResult result) {
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
