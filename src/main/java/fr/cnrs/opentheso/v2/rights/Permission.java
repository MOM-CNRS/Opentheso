package fr.cnrs.opentheso.v2.rights;

/**
 * Permissions métier du module de droits V2.
 * Seuils alignés :
 * <ul>
 *   <li>admin thésaurus / écriture API : rôle ≤ admin sur le thésaurus</li>
 *   <li>structure concept : rôle ≤ manager sur le thésaurus (ou capacité manager session)</li>
 *   <li>mutation concept / candidat : rôle ≤ contributor sur le thésaurus (ou capacité contributor session)</li>
 * </ul>
 */
public enum Permission {

    SUPER_ADMIN,

    MANAGE_PROJECT,

    MANAGE_THESAURUS,

    /** Alias sémantique de {@link #MANAGE_THESAURUS} (écriture API historique). */
    WRITE_THESAURUS,

    MANAGE_THESAURUS_STRUCTURE,

    CONTRIBUTE_ON_THESAURUS,

    TOOLBOX_EDITION,
    TOOLBOX_STATISTICS,
    TOOLBOX_MAINTENANCE,
    TOOLBOX_FLAGS,

    ACCESS_CANDIDAT,
    ACCESS_GRAPH,
    ACCESS_WORKSHOP,

    MUTATE_CONCEPT,
    MUTATE_CONCEPT_STRUCTURE
}