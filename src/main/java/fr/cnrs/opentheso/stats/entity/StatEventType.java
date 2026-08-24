package fr.cnrs.opentheso.stats.entity;

/**
 * Types d'événements suivis à des fins statistiques.
 * Correspond à la colonne event_type de la table stat_log_event.
 */
public enum StatEventType {
    CONCEPT_VIEW,
    COLLECTION_VIEW,
    GROUP_VIEW,
    API_CALL,

    /** Terme tapé dans l'autocomplete, sans aucun résultat trouvé. */
    SEARCH_NO_RESULT,

    /** Terme tapé dans l'autocomplete, suivi de la sélection d'une suggestion. */
    SEARCH_RESULT_SELECTED,

    /** Recherche appliquée. */
    SEARCH_APPLIED,
}

