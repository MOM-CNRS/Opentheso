package fr.cnrs.opentheso.shared.repository.projection;

public record EditionCollectionRow(
        String id,
        String label,
        boolean privateCollection,
        String parentId
) {
}
