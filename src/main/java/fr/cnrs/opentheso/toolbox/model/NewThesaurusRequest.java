package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;
import java.util.List;

public record NewThesaurusRequest(
        String title,
        String persistentNameThesaurus,
        String language,
        Integer projectId,
        boolean privateThesaurus,
        String organization,
        List<String> languages
) implements Serializable {

    public NewThesaurusRequest(
            String title,
            String persistentNameThesaurus,
            String language,
            Integer projectId,
            boolean privateThesaurus,
            String organization
    ) {
        this(title, persistentNameThesaurus, language, projectId, privateThesaurus, organization, List.of());
    }
}
