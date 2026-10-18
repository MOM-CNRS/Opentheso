package fr.cnrs.opentheso.shared.repository.projection;

public record ThesaurusLanguageRow(
        long id,
        String code,
        String codeFlag,
        String labelTheso,
        String displayLabel
) {
}
