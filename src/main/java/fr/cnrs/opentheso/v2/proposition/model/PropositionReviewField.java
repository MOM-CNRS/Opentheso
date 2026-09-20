package fr.cnrs.opentheso.v2.proposition.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Ligne de revue JSF (JavaBean, pas un record) : un champ concerné + ancien / nouveau.
 */
@Getter
@Setter
public class PropositionReviewField implements Serializable {

    private final PropositionFieldChange change;
    private final String title;
    private final String actionLabel;
    private final String actionCss;
    private final String lang;
    private final boolean hidden;
    private final String oldValue;
    private final boolean oldPresent;
    private String newValue;
    private boolean accepted;
    private boolean removed;

    public PropositionReviewField(
            PropositionFieldChange change,
            String title,
            String actionLabel,
            boolean accepted
    ) {
        this.change = change;
        this.title = title;
        this.actionLabel = actionLabel;
        this.actionCss = change == null ? "" : change.actionCss();
        this.lang = change == null ? "" : change.lang();
        this.hidden = change != null && change.hidden();
        this.oldValue = change == null ? "" : change.previousDisplay();
        this.oldPresent = change != null && change.hasPrevious();
        this.newValue = change == null ? "" : change.nextDisplay();
        this.accepted = accepted;
    }

    public PropositionFieldCategory getCategory() {
        return change == null ? null : change.category();
    }

    public boolean isDelete() {
        return change != null && change.isDelete();
    }

    public boolean isValueEditable() {
        return !isDelete();
    }

    public boolean isNewPresent() {
        return StringUtils.isNotBlank(newValue);
    }

    public boolean getNewPresent() {
        return isNewPresent();
    }

    /**
     * Change effectivement à appliquer après édition / suppression de ligne.
     */
    public PropositionFieldChange toAppliedChange() {
        if (change == null || removed) {
            return null;
        }
        if (change.isDelete()) {
            return change;
        }
        String edited = StringUtils.trimToNull(newValue);
        if (edited == null) {
            if (!oldPresent) {
                return null;
            }
            return new PropositionFieldChange(
                    change.category(),
                    PropositionFieldAction.DELETE,
                    change.lang(),
                    oldValue,
                    oldValue,
                    change.hidden());
        }
        if (change.isAdd() && !oldPresent) {
            return new PropositionFieldChange(
                    change.category(),
                    PropositionFieldAction.ADD,
                    change.lang(),
                    edited,
                    null,
                    change.hidden());
        }
        if (edited.equals(oldValue)) {
            return null;
        }
        return new PropositionFieldChange(
                change.category(),
                PropositionFieldAction.UPDATE,
                change.lang(),
                edited,
                StringUtils.trimToNull(oldValue),
                change.hidden());
    }
}
