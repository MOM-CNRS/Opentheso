package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotRelationCandidate(
        int line,
        String sourceLocalId,
        String targetLocalId,
        String sourceConceptId,
        String targetConceptId
) implements Serializable {
}
