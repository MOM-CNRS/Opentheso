package fr.cnrs.opentheso.v2.concept.alignment.model;

/**
 * Ligne du tableau résumé (branche) : soit un alignement réel, soit un
 * placeholder sans URI pour garder le groupe concept visible.
 */
public record AlignmentAdminRow(
        String conceptId,
        String conceptLabel,
        Integer alignmentId,
        String targetUri,
        String typeLabel,
        int typeId,
        String sourceName,
        boolean urlAvailable
) {

    public boolean isPlaceholder() {
        return alignmentId == null || targetUri == null;
    }

    public String getConceptId() {
        return conceptId;
    }

    public String getConceptLabel() {
        return conceptLabel;
    }

    public Integer getAlignmentId() {
        return alignmentId;
    }

    public String getTargetUri() {
        return targetUri;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public int getTypeId() {
        return typeId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public boolean isUrlAvailable() {
        return urlAvailable;
    }
}
