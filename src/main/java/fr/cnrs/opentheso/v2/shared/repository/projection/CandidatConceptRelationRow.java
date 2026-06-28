package fr.cnrs.opentheso.v2.shared.repository.projection;

public record CandidatConceptRelationRow(
        String conceptId,
        String relatedConceptId,
        String label
) {
}
