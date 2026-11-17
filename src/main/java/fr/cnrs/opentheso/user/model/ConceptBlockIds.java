package fr.cnrs.opentheso.user.model;

import java.util.LinkedHashSet;
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

    /**
     * Blocs réordonnables en plus de l'ordre historique.
     * Ils ne sont pas ajoutés d'office : absents, l'écran les place
     * (Branche juste après Contexte). Une fois enregistrés, ils gardent leur place.
     */
    private static final List<String> OPTIONAL_ORDER = List.of(
            "branche",
            "metadonnees"
    );

    public static final Set<String> ALL = allIds();

    private ConceptBlockIds() {
    }

    private static Set<String> allIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>(DEFAULT_ORDER);
        ids.addAll(OPTIONAL_ORDER);
        return Set.copyOf(ids);
    }
}
