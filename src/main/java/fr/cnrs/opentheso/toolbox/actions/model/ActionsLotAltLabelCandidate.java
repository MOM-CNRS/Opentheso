package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotAltLabelCandidate(
        int line,
        String localId,
        String conceptId,
        String label,
        String lang
) implements Serializable {
}
