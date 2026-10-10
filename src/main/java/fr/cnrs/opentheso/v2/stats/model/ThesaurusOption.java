package fr.cnrs.opentheso.v2.stats.model;

public record ThesaurusOption(
        String id,
        String label,
        String projectName,
        boolean privateThesaurus
) {
}
