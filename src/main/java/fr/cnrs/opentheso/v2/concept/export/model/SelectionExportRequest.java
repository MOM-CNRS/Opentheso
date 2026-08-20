package fr.cnrs.opentheso.v2.concept.export.model;

import java.util.List;

public record SelectionExportRequest(
        String thesaurusId,
        String thesaurusTitle,
        List<String> conceptIds,
        String format,
        boolean includeDescendants,
        boolean wholeThesaurus,
        boolean clearHtml,
        boolean includeImages,
        boolean filterByGroup,
        boolean exportByGroup,
        List<String> groupIds,
        List<String> languageCodes,
        String csvDelimiter,
        String pdfType,
        String language1,
        String language2
) {
    public SelectionExportRequest {
        conceptIds = conceptIds == null ? List.of() : List.copyOf(conceptIds);
        groupIds = groupIds == null ? List.of() : List.copyOf(groupIds);
        languageCodes = languageCodes == null ? List.of() : List.copyOf(languageCodes);
        csvDelimiter = csvDelimiter == null || csvDelimiter.isBlank() ? "," : csvDelimiter;
        pdfType = pdfType == null || pdfType.isBlank() ? "hierarchical" : pdfType;
    }

    public static SelectionExportRequest of(
            String thesaurusId,
            List<String> conceptIds,
            String format,
            boolean includeDescendants,
            boolean wholeThesaurus
    ) {
        return new SelectionExportRequest(
                thesaurusId,
                null,
                conceptIds,
                format,
                includeDescendants,
                wholeThesaurus,
                false,
                false,
                false,
                false,
                List.of(),
                List.of(),
                ",",
                "hierarchical",
                null,
                null
        );
    }
}
