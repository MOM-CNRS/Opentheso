package fr.cnrs.opentheso.v2.concept.export.model;

public record SelectionExportStatus(
        String status,
        int progress,
        int done,
        int total,
        String message,
        String filename,
        String error,
        String phase,
        String phaseLabel,
        int phaseIndex,
        int phaseCount,
        long bytes,
        boolean downloadable,
        long startedAt
) {
    public static SelectionExportStatus idle() {
        return new SelectionExportStatus(
                "idle", 0, 0, 0, "", null, null, "resolve", "Préparer", 0, 4, 0, false, 0
        );
    }
}
