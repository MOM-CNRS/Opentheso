package fr.cnrs.opentheso.v2.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreateAdminUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        boolean alertMail,
        Integer roleId,
        Integer projectId,
        boolean limitedOnThesaurus,
        List<String> thesaurusIds,
        String password,
        String passwordConfirmation,
        List<ProjectRoleAssignmentRequest> projectRoles,
        boolean apiKeyAuthorized,
        boolean apiKeyNeverExpire,
        LocalDate apiKeyExpiresAt,
        String institution,
        String creationMode,
        Boolean active
) {
    public CreateAdminUserRequest {
        if (thesaurusIds == null) {
            thesaurusIds = List.of();
        }
        if (projectRoles == null) {
            projectRoles = List.of();
        }
    }

    public record ProjectRoleAssignmentRequest(int projectId, int roleId) {
    }
}
