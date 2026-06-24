package fr.cnrs.opentheso.v2.admin.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateAdminUserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        boolean alertMail,
        Integer roleId,
        Integer projectId,
        boolean limitedOnThesaurus,
        List<String> thesaurusIds,
        @NotBlank String password,
        @NotBlank String passwordConfirmation
) {
    public CreateAdminUserRequest {
        if (thesaurusIds == null) {
            thesaurusIds = List.of();
        }
    }
}
