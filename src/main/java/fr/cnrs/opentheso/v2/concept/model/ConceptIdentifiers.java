package fr.cnrs.opentheso.v2.concept.model;

public record ConceptIdentifiers(
        String internalPermalinkUrl,
        String originalUri,
        String permanentId,
        String qrCodeValue,
        boolean showOriginalUri,
        boolean showQrCode
) {

    public String getInternalPermalinkUrl() {
        return internalPermalinkUrl;
    }

    public String getOriginalUri() {
        return originalUri;
    }

    public String getPermanentId() {
        return permanentId;
    }

    public String getQrCodeValue() {
        return qrCodeValue;
    }

    public boolean isShowOriginalUri() {
        return showOriginalUri;
    }

    public boolean isShowQrCode() {
        return showQrCode;
    }
}
