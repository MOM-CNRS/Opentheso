package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public record ActionsLotNoteValidationResult(
        boolean success,
        String errorMessage,
        int linesRead,
        int validCount,
        int errorCount,
        int ignoredCount,
        List<ActionsLotLineError> errors,
        List<ActionsLotNoteCandidate> validCandidates
) implements Serializable {

    public static ActionsLotNoteValidationResult failure(String message) {
        return new ActionsLotNoteValidationResult(
                false, message, 0, 0, 0, 0,
                Collections.emptyList(), Collections.emptyList()
        );
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }
}
