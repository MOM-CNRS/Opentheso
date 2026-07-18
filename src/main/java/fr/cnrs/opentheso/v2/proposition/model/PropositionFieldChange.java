package fr.cnrs.opentheso.v2.proposition.model;

import java.io.Serializable;

public record PropositionFieldChange(
        PropositionFieldCategory category,
        PropositionFieldAction action,
        String lang,
        String value,
        String oldValue,
        boolean hidden
) implements Serializable {

    public boolean isAdd() {
        return action == PropositionFieldAction.ADD;
    }

    public boolean isUpdate() {
        return action == PropositionFieldAction.UPDATE;
    }

    public boolean isDelete() {
        return action == PropositionFieldAction.DELETE;
    }
}
