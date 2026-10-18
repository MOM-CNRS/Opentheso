package fr.cnrs.opentheso.shared.repository.projection;

public record EditionThesaurusDetailsRow(
        String id,
        String title,
        String arkId,
        boolean privateThesaurus,
        String sourceLang
) {
}
