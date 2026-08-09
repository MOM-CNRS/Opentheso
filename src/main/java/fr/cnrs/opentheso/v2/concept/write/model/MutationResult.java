package fr.cnrs.opentheso.v2.concept.write.model;

public record MutationResult(
        boolean success,
        MutationOutcome outcome,
        String message,
        String createdConceptId,
        boolean warning
) {

    public static MutationResult ok(String message) {
        return new MutationResult(true, MutationOutcome.OK, message, null, false);
    }

    public static MutationResult ok(String message, String createdConceptId) {
        return new MutationResult(true, MutationOutcome.OK, message, createdConceptId, false);
    }

    public static MutationResult okWithWarning(String message) {
        return new MutationResult(true, MutationOutcome.OK, message, null, true);
    }

    public static MutationResult duplicate(String message) {
        return new MutationResult(false, MutationOutcome.DUPLICATE_LABEL, message, null, false);
    }

    public static MutationResult validationError(String message) {
        return new MutationResult(false, MutationOutcome.VALIDATION_ERROR, message, null, false);
    }

    public static MutationResult forbidden(String message) {
        return new MutationResult(false, MutationOutcome.FORBIDDEN, message, null, false);
    }

    public static MutationResult failure(String message) {
        return new MutationResult(false, MutationOutcome.FAILURE, message, null, false);
    }
}
