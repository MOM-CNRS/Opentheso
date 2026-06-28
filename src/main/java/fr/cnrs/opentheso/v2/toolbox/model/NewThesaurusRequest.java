package fr.cnrs.opentheso.v2.toolbox.model;

public record NewThesaurusRequest(
        String title,
        String language,
        Integer projectId
) {
}
