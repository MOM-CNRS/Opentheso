package fr.cnrs.opentheso.v2.shared.repository.projection;

public record EditionThesaurusDetailsRow(
        String id,
        String title,
        String arkId,
        boolean privateThesaurus,
        String sourceLang
) {
}
