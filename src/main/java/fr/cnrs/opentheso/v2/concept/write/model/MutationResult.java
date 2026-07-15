package fr.cnrs.opentheso.v2.concept.write.model;

public record MutationResult(
        boolean success,
        MutationOutcome outcome,
        String message,
        String createdConceptId
) {

    public static MutationResult ok(String message) {
        return new MutationResult(true, MutationOutcome.OK, message, null);
    }

    public static MutationResult ok(String message, String createdConceptId) {
        return new MutationResult(true, MutationOutcome.OK, message, createdConceptId);
    }

    public static MutationResult duplicate(String message) {
        return new MutationResult(false, MutationOutcome.DUPLICATE_LABEL, message, null);
    }

    public static MutationResult validationError(String message) {
        return new MutationResult(false, MutationOutcome.VALIDATION_ERROR, message, null);
    }

    public static MutationResult forbidden(String message) {
        return new MutationResult(false, MutationOutcome.FORBIDDEN, message, null);
    }

    public static MutationResult failure(String message) {
        return new MutationResult(false, MutationOutcome.FAILURE, message, null);
    }
}
