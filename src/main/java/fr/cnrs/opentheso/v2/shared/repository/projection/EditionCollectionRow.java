package fr.cnrs.opentheso.v2.shared.repository.projection;

public record EditionCollectionRow(
        String id,
        String label,
        boolean privateCollection,
        String parentId
) {
}
