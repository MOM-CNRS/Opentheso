package fr.cnrs.opentheso.proposition.exception;

public class PropositionNotFoundException extends RuntimeException {

    public PropositionNotFoundException(int propositionId) {
        super("Proposition introuvable : " + propositionId);
    }
}
