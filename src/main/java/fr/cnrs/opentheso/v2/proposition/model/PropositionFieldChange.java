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

    public PropositionFieldCategory getCategory() {
        return category;
    }

    public PropositionFieldAction getAction() {
        return action;
    }

    public String getLang() {
        return lang;
    }

    public String getValue() {
        return value;
    }

    public String getOldValue() {
        return oldValue;
    }

    public boolean isHidden() {
        return hidden;
    }

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
