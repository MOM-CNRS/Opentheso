package fr.cnrs.opentheso.v2.user.model;

import java.util.Set;
import java.io.Serializable;

public record TableColPref(Set<String> selected) implements Serializable {

    public static TableColPref defaults() {
        return new TableColPref(TableColIds.DEFAULT_SELECTED);
    }

    public boolean contains(String colId) {
        return selected != null && selected.contains(colId);
    }
}
