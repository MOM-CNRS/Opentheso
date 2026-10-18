package fr.cnrs.opentheso.shared.repository.projection;

public record CandidatConceptRelationRow(
        String conceptId,
        String relatedConceptId,
        String label
) {
}
