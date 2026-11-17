package fr.cnrs.opentheso.shared.model;

import java.util.List;

public record PickerFooterSettings(
        String brandLabel,
        String brandUrl,
        boolean customImage,
        long imageVersion,
        List<PickerFooterLink> links
) {
}
