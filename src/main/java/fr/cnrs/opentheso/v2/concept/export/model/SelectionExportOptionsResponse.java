package fr.cnrs.opentheso.v2.concept.export.model;

import java.util.List;

public record SelectionExportOptionsResponse(
        String workLanguage,
        String thesaurusTitle,
        List<LangItem> languages,
        List<GroupItem> groups
) {
    public record LangItem(String code, String label) {
    }

    public record GroupItem(String id, String label) {
    }
}
