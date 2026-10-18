package fr.cnrs.opentheso.publicapi.group.api.dto;

import java.util.List;

public record GroupBranchTreeEntryResponse(
        String groupId,
        String label,
        List<PathStep> pathToRoot
) {
    public record PathStep(String groupId, String label) {
    }
}
