package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotNoteCandidate(
        int line,
        String localId,
        String conceptId,
        String typeCode,
        String lang,
        String value
) implements Serializable {
}
