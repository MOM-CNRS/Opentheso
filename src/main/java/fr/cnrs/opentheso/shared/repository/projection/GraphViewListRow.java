package fr.cnrs.opentheso.shared.repository.projection;

public record GraphViewListRow(
        int id,
        String name,
        String description,
        String exportsJson
) {
}
