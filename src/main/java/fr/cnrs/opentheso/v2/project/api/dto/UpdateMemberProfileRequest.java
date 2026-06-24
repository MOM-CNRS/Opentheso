package fr.cnrs.opentheso.v2.project.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateMemberProfileRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        boolean alertMail,
        String institution,
        boolean active
) {
}
