package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotArkCandidate(
        int line,
        String localId,
        String conceptId,
        String arkId
) implements Serializable {
}
