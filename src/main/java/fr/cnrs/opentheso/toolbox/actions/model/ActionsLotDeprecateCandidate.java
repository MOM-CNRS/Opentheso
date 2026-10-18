package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotDeprecateCandidate(
        int line,
        String localId,
        String conceptId,
        String replacedByLocalId,
        String replacedByConceptId,
        String note,
        String noteLang
) implements Serializable {
}
