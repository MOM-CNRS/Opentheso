package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;

public record ActionsLotApplyResult(
        boolean success,
        String message,
        int linesRead,
        int applied,
        int rejected
) implements Serializable {

    public static ActionsLotApplyResult failure(String message) {
        return new ActionsLotApplyResult(false, message, 0, 0, 0);
    }
}
