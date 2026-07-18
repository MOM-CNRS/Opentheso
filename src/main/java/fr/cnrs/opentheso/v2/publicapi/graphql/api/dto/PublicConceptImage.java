package fr.cnrs.opentheso.v2.publicapi.graphql.api.dto;

public record PublicConceptImage(
        String uri,
        String imageName,
        String copyright,
        String creator
) {
}
