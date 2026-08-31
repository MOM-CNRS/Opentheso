package fr.cnrs.opentheso.v2.user.model;

import java.util.Set;

public record TreeStatusPref(Set<String> selected) {

    public static TreeStatusPref defaults() {
        return new TreeStatusPref(TreeStatusIds.DEFAULT_SELECTED);
    }

    public boolean contains(String statusId) {
        return selected != null && selected.contains(statusId);
    }
}
