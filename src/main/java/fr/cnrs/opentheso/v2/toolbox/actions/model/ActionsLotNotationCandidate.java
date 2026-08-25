package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotNotationCandidate(
        int line,
        String localId,
        String conceptId,
        String notation
) implements Serializable {
}
