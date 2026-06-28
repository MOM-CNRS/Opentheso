package fr.cnrs.opentheso.v2.toolbox.model;

import java.util.List;

public record NewThesaurusFormOptions(
        List<LanguageOption> languages,
        List<ProjectOption> projects,
        boolean superAdmin
) {
}
