package fr.cnrs.opentheso.v2.toolbox.model;

public record NewThesaurusRequest(
        String title,
        String persistentNameThesaurus,
        String language,
        Integer projectId
) {
}
