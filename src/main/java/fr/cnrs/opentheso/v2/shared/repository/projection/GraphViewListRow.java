package fr.cnrs.opentheso.v2.shared.repository.projection;

public record GraphViewListRow(
        int id,
        String name,
        String description,
        String exportsJson
) {
}
