package fr.cnrs.opentheso.v2.proposition.model;

import java.io.Serializable;

public record PropositionAcceptance(
        boolean preferredLabel,
        boolean synonyms,
        boolean translations,
        boolean note,
        boolean definition,
        boolean changeNote,
        boolean scopeNote,
        boolean editorialNote,
        boolean example,
        boolean historyNote
) implements Serializable {

    public static PropositionAcceptance none() {
        return new PropositionAcceptance(false, false, false, false, false, false, false, false, false, false);
    }

    public boolean isAccepted(PropositionFieldCategory category) {
        return switch (category) {
            case NOM -> preferredLabel;
            case SYNONYME -> synonyms;
            case TRADUCTION -> translations;
            case NOTE -> note;
            case DEFINITION -> definition;
            case CHANGE_NOTE -> changeNote;
            case SCOPE -> scopeNote;
            case EDITORIAL_NOTE -> editorialNote;
            case EXAMPLE -> example;
            case HISTORY -> historyNote;
        };
    }
}
