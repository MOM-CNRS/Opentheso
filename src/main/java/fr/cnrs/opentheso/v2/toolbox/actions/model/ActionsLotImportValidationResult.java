package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public record ActionsLotImportValidationResult<C>(
        boolean success,
        String errorMessage,
        int linesRead,
        int validCount,
        int errorCount,
        int ignoredCount,
        List<ActionsLotLineError> errors,
        List<C> validCandidates,
        String context
) implements Serializable {

    public ActionsLotImportValidationResult(
            boolean success,
            String errorMessage,
            int linesRead,
            int validCount,
            int errorCount,
            int ignoredCount,
            List<ActionsLotLineError> errors,
            List<C> validCandidates
    ) {
        this(success, errorMessage, linesRead, validCount, errorCount, ignoredCount, errors, validCandidates, null);
    }

    public static <C> ActionsLotImportValidationResult<C> failure(String message) {
        return new ActionsLotImportValidationResult<>(
                false, message, 0, 0, 0, 0,
                Collections.emptyList(), Collections.emptyList(), null
        );
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }
}
