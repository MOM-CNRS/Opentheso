package fr.cnrs.opentheso.v2.proposition.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Ligne de note en consultation : changement + case à cocher d'acceptation.
 */
@Getter
@Setter
public class NoteReviewEntry implements Serializable {

    private final PropositionFieldChange change;
    private final String messageKey;
    private boolean accepted;

    public NoteReviewEntry(PropositionFieldChange change, String messageKey, boolean accepted) {
        this.change = change;
        this.messageKey = messageKey;
        this.accepted = accepted;
    }
}
