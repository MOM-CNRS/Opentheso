package fr.cnrs.opentheso.stats.model;

public record ThesaurusOption(
        String id,
        String label,
        String projectName,
        boolean privateThesaurus
) {
}
