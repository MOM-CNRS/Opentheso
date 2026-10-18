package fr.cnrs.opentheso.publicapi.graphql.api.dto;

public record PublicConceptRelation(
        String conceptId,
        String label,
        String arkId
) {
}
