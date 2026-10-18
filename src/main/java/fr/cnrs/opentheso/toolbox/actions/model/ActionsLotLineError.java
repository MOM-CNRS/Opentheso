package fr.cnrs.opentheso.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotLineError(
        int line,
        String identifier,
        String column,
        String message
) implements Serializable {
}
