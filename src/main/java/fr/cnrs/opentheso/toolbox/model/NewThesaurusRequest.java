package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;

public record NewThesaurusRequest(
        String title,
        String persistentNameThesaurus,
        String language,
        Integer projectId,
        boolean privateThesaurus,
        String organization
) implements Serializable {
}
