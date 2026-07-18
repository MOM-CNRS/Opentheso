package fr.cnrs.opentheso.v2.publicapi.group.api.dto;

import java.util.List;

public record GroupSummaryResponse(
        String groupId,
        List<Translation> labels
) {
    public record Translation(String lang, String title) {
    }
}
