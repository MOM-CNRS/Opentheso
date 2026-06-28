package fr.cnrs.opentheso.v2.concept.model;

public record ConceptTreeNode(
        String id,
        String thesaurusId,
        String label,
        String notation,
        String status,
        boolean hasChildren
) {}
