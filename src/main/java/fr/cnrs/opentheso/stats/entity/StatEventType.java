package fr.cnrs.opentheso.stats.entity;

/**
 * Types d'événements suivis à des fins statistiques.
 * Correspond à la colonne event_type de la table stat_log_event.
 */
public enum StatEventType {
    CONCEPT_VIEW,
    COLLECTION_VIEW,
    GROUP_VIEW,
    API_CALL
}

