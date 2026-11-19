package fr.cnrs.opentheso.suggestion.model;

/**
 * Réglages d'affichage de la barre de gauche. Les quatre entrées sont visibles par défaut.
 */
public record InstanceFeatures(
        boolean suggestionEnabled,
        boolean languageVisible,
        boolean restApiVisible,
        boolean graphApiVisible
) {
    public static InstanceFeatures defaults() {
        return new InstanceFeatures(true, true, true, true);
    }
}
