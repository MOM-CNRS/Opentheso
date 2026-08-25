package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotCollectionCandidate(
        int line,
        String localId,
        String conceptId,
        String groupId
) implements Serializable {
}
