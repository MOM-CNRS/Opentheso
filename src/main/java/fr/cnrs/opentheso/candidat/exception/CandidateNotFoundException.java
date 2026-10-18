package fr.cnrs.opentheso.candidat.exception;

public class CandidateNotFoundException extends RuntimeException {

    public CandidateNotFoundException(String conceptId) {
        super("Candidat introuvable : " + conceptId);
    }
}
