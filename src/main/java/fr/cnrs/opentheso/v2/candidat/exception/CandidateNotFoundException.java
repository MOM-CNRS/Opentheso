package fr.cnrs.opentheso.v2.candidat.exception;

public class CandidateNotFoundException extends RuntimeException {

    public CandidateNotFoundException(String conceptId) {
        super("Candidat introuvable : " + conceptId);
    }
}
