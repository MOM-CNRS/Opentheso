package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotCollectionCandidate(
        int line,
        String localId,
        String conceptId,
        String groupId
) implements Serializable {
}
