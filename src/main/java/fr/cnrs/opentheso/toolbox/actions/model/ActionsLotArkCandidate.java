package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotArkCandidate(
        int line,
        String localId,
        String conceptId,
        String arkId
) implements Serializable {
}
