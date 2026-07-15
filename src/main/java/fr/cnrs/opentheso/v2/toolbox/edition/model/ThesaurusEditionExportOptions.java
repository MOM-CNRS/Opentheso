package fr.cnrs.opentheso.v2.toolbox.edition.model;

import java.util.List;

public record ThesaurusEditionExportOptions(
        boolean filterByGroup,
        List<String> selectedGroupIds,
        boolean clearHtml
) {
    public static ThesaurusEditionExportOptions full() {
        return new ThesaurusEditionExportOptions(false, List.of(), false);
    }
}
