package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotConceptCandidate(
        int line,
        String identifier,
        String type
) implements Serializable {
}
