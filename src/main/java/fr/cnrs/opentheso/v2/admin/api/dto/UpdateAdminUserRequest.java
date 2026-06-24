package fr.cnrs.opentheso.v2.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateAdminUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        boolean alertMail
) {
}
