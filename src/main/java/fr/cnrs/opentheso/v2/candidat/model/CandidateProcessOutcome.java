package fr.cnrs.opentheso.v2.candidat.model;

public record CandidateProcessOutcome(boolean success) {
    public static CandidateProcessOutcome ok() {
        return new CandidateProcessOutcome(true);
    }

    public static CandidateProcessOutcome fail() {
        return new CandidateProcessOutcome(false);
    }

    public boolean isFailure() {
        return !success;
    }
}
