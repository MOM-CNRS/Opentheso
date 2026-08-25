package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotLineError(
        int line,
        String identifier,
        String column,
        String message
) implements Serializable {
}
