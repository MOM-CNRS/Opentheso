package fr.cnrs.opentheso.v2.project.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateProjectMemberRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        String institution,
        boolean alertMail,
        @NotNull Integer roleId,
        boolean limitedOnThesaurus,
        List<String> thesaurusIds,
        String password,
        String passwordConfirmation,
        String creationMode
) {
    public CreateProjectMemberRequest {
        if (creationMode == null || creationMode.isBlank()) {
            creationMode = "DIRECT";
        }
        if (thesaurusIds == null) {
            thesaurusIds = List.of();
        }
    }
}
