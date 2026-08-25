package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotCompareCandidate(
        int line,
        String originalPrefLabel
) implements Serializable {
}
