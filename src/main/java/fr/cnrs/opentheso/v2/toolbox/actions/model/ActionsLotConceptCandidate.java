package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotConceptCandidate(
        int line,
        String identifier,
        String type
) implements Serializable {
}
