package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotAltLabelCandidate(
        int line,
        String localId,
        String conceptId,
        String label,
        String lang
) implements Serializable {
}
