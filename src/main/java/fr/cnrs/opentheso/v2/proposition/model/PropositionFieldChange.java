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

    public boolean hasPrevious() {
        return !previousDisplay().isBlank();
    }

    public boolean hasNext() {
        return !nextDisplay().isBlank();
    }

    /** Valeur à afficher dans la colonne « ancien » (vide pour un ajout). */
    public String previousDisplay() {
        if (isAdd()) {
            return "";
        }
        if (oldValue != null && !oldValue.isBlank()) {
            return oldValue;
        }
        return value == null ? "" : value;
    }

    /** Valeur à afficher dans la colonne « nouveau » (vide pour une suppression). */
    public String nextDisplay() {
        if (isDelete()) {
            return "";
        }
        return value == null ? "" : value;
    }

    public String actionCss() {
        if (isAdd()) {
            return "is-add";
        }
        if (isDelete()) {
            return "is-delete";
        }
        return "is-update";
    }
}
