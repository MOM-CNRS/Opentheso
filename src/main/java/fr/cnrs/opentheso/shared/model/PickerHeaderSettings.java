package fr.cnrs.opentheso.shared.model;

public record PickerHeaderSettings(
        boolean visible,
        String label,
        String url,
        boolean customImage,
        long imageVersion
) {
}
