package fr.cnrs.opentheso.v2.user.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TreeStatusIds {

    public static final String VALIDE = "valide";
    public static final String INSERE = "insere";
    public static final String CANDIDAT = "candidat";
    public static final String REJETE = "rejete";
    public static final String DEPRECIE = "deprecie";

    public static final List<String> ALL = List.of(VALIDE, INSERE, CANDIDAT, REJETE, DEPRECIE);

    public static final Set<String> DEFAULT_SELECTED = Set.of(VALIDE, INSERE, CANDIDAT);

    public static final Map<String, List<String>> GROUPS = Map.of(
            "actif", List.of(VALIDE, INSERE),
            "candidat", List.of(CANDIDAT),
            "inactif", List.of(REJETE, DEPRECIE)
    );

    private TreeStatusIds() {
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
