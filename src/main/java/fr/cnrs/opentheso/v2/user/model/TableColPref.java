package fr.cnrs.opentheso.v2.user.model;

import java.util.Set;

public record TableColPref(Set<String> selected) {

    public static TableColPref defaults() {
        return new TableColPref(TableColIds.DEFAULT_SELECTED);
    }

    public boolean contains(String colId) {
        return selected != null && selected.contains(colId);
    }
}
