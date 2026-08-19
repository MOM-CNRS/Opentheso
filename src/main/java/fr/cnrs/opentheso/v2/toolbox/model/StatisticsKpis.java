package fr.cnrs.opentheso.v2.toolbox.model;

/**
 * Compteurs légers du thésaurus. {@code candidates} = propositions en attente.
 */
public record StatisticsKpis(
        int concepts,
        int candidates,
        int collections,
        int languages
) {
}
