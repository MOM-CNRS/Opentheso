package fr.cnrs.opentheso.v2.toolbox.edition.model;

import java.util.List;
import java.io.Serializable;

public record ThesaurusEditionExportOptions(
        boolean filterByGroup,
        List<String> selectedGroupIds,
        boolean clearHtml
) implements Serializable {
    public static ThesaurusEditionExportOptions full() {
        return new ThesaurusEditionExportOptions(false, List.of(), false);
    }
}
