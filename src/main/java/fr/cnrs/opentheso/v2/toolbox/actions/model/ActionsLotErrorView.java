package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.util.Collections;
import java.util.List;

/**
 * Limite le rendu JSF des tableaux d'erreurs (le compteur reste complet).
 */
public final class ActionsLotErrorView {

    public static final int LIMIT = 80;

    private ActionsLotErrorView() {
    }

    public static List<ActionsLotLineError> visibleErrors(List<ActionsLotLineError> errors) {
        if (errors == null || errors.isEmpty()) {
            return Collections.emptyList();
        }
        if (errors.size() <= LIMIT) {
            return errors;
        }
        return List.copyOf(errors.subList(0, LIMIT));
    }
}
