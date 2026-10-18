package fr.cnrs.opentheso.concept.write.persistence;

public record ConceptSnapshot(
        String conceptId,
        String thesaurusId,
        String idArk,
        String status,
        String notation,
        boolean topConcept
) {
}
