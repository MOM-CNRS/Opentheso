package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public record ActionsLotAltLabelValidationResult(
        boolean success,
        String errorMessage,
        int linesRead,
        int validCount,
        int errorCount,
        int ignoredCount,
        List<ActionsLotLineError> errors,
        List<ActionsLotAltLabelCandidate> validCandidates
) implements Serializable, ActionsLotCheckOutcome {

    public static ActionsLotAltLabelValidationResult failure(String message) {
        return new ActionsLotAltLabelValidationResult(
                false, message, 0, 0, 0, 0,
                Collections.emptyList(), Collections.emptyList()
        );
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }
}
