package fr.cnrs.opentheso.suggestion.model;

/**
 * Réglages d'affichage de la barre de gauche et interrupteur de l'API V1. Tout est actif par défaut.
 */
public record InstanceFeatures(
        boolean suggestionEnabled,
        boolean languageVisible,
        boolean restApiVisible,
        boolean graphApiVisible,
        boolean apiV1Enabled
) {
    public static InstanceFeatures defaults() {
        return new InstanceFeatures(true, true, true, true, true);
    }
}
