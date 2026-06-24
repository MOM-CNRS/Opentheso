package fr.cnrs.opentheso.v2.project.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateProjectMemberRoleRequest(
        @NotNull Integer roleId,
        boolean limitedOnThesaurus,
        List<String> thesaurusIds
) {
    public UpdateProjectMemberRoleRequest {
        if (thesaurusIds == null) {
            thesaurusIds = List.of();
        }
    }
}
