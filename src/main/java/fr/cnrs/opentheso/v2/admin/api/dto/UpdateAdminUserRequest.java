package fr.cnrs.opentheso.v2.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record UpdateAdminUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        boolean alertMail,
        String institution,
        Boolean active,
        Boolean makeSuperAdmin,
        String password,
        String passwordConfirmation,
        List<CreateAdminUserRequest.ProjectRoleAssignmentRequest> projectRoles,
        Boolean apiKeyAuthorized,
        boolean apiKeyNeverExpire,
        LocalDate apiKeyExpiresAt
) {
}
