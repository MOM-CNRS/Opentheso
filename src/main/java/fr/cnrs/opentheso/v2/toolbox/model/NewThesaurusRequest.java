package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record NewThesaurusRequest(
        String title,
        String persistentNameThesaurus,
        String language,
        Integer projectId
) implements Serializable {
}
