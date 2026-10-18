package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotNotationCandidate(
        int line,
        String localId,
        String conceptId,
        String notation
) implements Serializable {
}
