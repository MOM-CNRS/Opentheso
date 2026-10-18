package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotImageCandidate(
        int line,
        String localId,
        String conceptId,
        String uri,
        String title,
        String rights,
        String creator
) implements Serializable {
}
