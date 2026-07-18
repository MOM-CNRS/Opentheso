package fr.cnrs.opentheso.v2.publicapi.graphql.api.dto;

import java.util.List;

public record PublicConceptNode(
        String conceptId,
        String thesaurusId,
        String prefLabel,
        String notation,
        String conceptType,
        String status,
        String arkId,
        String created,
        String modified,

        List<PublicConceptLabel> translations,
        List<String> synonyms,
        List<String> hiddenSynonyms,

        List<PublicConceptRelation> broaders,
        List<PublicConceptRelation> narrowers,
        List<PublicConceptRelation> relateds,
        List<PublicConceptRelation> collections,
        List<PublicConceptRelation> facets,
        List<PublicConceptRelation> replacedBy,
        List<PublicConceptRelation> replaces,
        List<PublicConceptRelation> exactMatches,

        List<PublicConceptNote> definitions,
        List<PublicConceptNote> examples,
        List<PublicConceptNote> editorialNotes,
        List<PublicConceptNote> scopeNotes,
        List<PublicConceptNote> historyNotes,
        List<PublicConceptNote> changeNotes,

        List<PublicConceptImage> images,
        List<PublicConceptGps> gpsPoints
) {
}
