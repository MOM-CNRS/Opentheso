package fr.cnrs.opentheso.v2.user.model;

import java.util.Set;
import java.io.Serializable;

public record TreeStatusPref(Set<String> selected) implements Serializable {

    public static TreeStatusPref defaults() {
        return new TreeStatusPref(TreeStatusIds.DEFAULT_SELECTED);
    }

    public boolean contains(String statusId) {
        return selected != null && selected.contains(statusId);
    }
}
