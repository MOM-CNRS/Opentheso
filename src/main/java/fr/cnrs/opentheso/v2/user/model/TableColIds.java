package fr.cnrs.opentheso.v2.user.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TableColIds {

    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String NOTATION = "notation";
    public static final String PATH = "path";

    public static final List<String> ALL = List.of(STATUS, TYPE, NOTATION, PATH);

    public static final Set<String> DEFAULT_SELECTED = Set.copyOf(ALL);

    private TableColIds() {
    }

    public static Set<String> normalizeSelected(Iterable<String> selected) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (selected != null) {
            for (String id : selected) {
                if (id != null && ALL.contains(id)) {
                    normalized.add(id);
                }
            }
        }
        return Set.copyOf(normalized);
    }
}
