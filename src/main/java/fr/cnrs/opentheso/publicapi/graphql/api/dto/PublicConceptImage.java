package fr.cnrs.opentheso.publicapi.graphql.api.dto;

public record PublicConceptImage(
        String uri,
        String imageName,
        String copyright,
        String creator
) {
}
