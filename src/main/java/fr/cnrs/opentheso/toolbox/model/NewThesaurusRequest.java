package fr.cnrs.opentheso.toolbox.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record NewThesaurusRequest(
        String title,
        String persistentNameThesaurus,
        String language,
        Integer projectId,
        boolean privateThesaurus,
        String organization,
        List<String> languages,
        boolean master,
        String subject,
        Map<String, String> titles
) implements Serializable {

    public NewThesaurusRequest(
            String title,
            String persistentNameThesaurus,
            String language,
            Integer projectId,
            boolean privateThesaurus,
            String organization
    ) {
        this(title, persistentNameThesaurus, language, projectId, privateThesaurus, organization, List.of(), true, "");
    }

    public NewThesaurusRequest(
            String title,
            String persistentNameThesaurus,
            String language,
            Integer projectId,
            boolean privateThesaurus,
            String organization,
            List<String> languages
    ) {
        this(
                title,
                persistentNameThesaurus,
                language,
                projectId,
                privateThesaurus,
                organization,
                languages == null ? List.of() : languages,
                true,
                ""
        );
    }

    public NewThesaurusRequest(
            String title,
            String persistentNameThesaurus,
            String language,
            Integer projectId,
            boolean privateThesaurus,
            String organization,
            List<String> languages,
            boolean master,
            String subject
    ) {
        this(
                title,
                persistentNameThesaurus,
                language,
                projectId,
                privateThesaurus,
                organization,
                languages == null ? List.of() : languages,
                master,
                subject == null ? "" : subject,
                Map.of()
        );
    }
}
