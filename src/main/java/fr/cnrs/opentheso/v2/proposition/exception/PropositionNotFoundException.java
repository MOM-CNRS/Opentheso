package fr.cnrs.opentheso.v2.proposition.exception;

public class PropositionNotFoundException extends RuntimeException {

    public PropositionNotFoundException(int propositionId) {
        super("Proposition introuvable : " + propositionId);
    }
}
