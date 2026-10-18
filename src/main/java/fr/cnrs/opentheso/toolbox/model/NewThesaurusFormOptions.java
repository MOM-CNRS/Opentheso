package fr.cnrs.opentheso.toolbox.model;

import java.util.List;
import java.io.Serializable;

public record NewThesaurusFormOptions(
        List<LanguageOption> languages,
        List<ProjectOption> projects,
        boolean superAdmin
) implements Serializable {
}
