package fr.cnrs.opentheso.v2.user.model;

import java.util.List;
import java.util.Set;

/**
 * Identifiants des blocs de la fiche concept, dans l'ordre de création par défaut.
 */
public final class ConceptBlockIds {

    public static final List<String> DEFAULT_ORDER = List.of(
            "contexte",
            "collections",
            "relations",
            "relPerso",
            "traductions",
            "notes",
            "ressources",
            "alignement",
            "identifiants",
            "temporel"
    );

    public static final Set<String> ALL = Set.copyOf(DEFAULT_ORDER);

    private ConceptBlockIds() {
    }
}
