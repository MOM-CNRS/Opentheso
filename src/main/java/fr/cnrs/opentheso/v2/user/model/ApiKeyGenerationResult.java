package fr.cnrs.opentheso.v2.user.model;

import java.io.Serializable;

/**
 * Résultat d'une régénération de clé API.
 * La clé en clair n'est affichée qu'une seule fois côté UI.
 */
public record ApiKeyGenerationResult(
        String plainTextKey,
        UserProfile profile
) implements Serializable {
}
