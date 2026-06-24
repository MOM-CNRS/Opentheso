package fr.cnrs.opentheso.v2.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateLimitedMemberRoleRequest(
        @NotNull Integer oldRoleId,
        @NotNull Integer newRoleId,
        @NotBlank String thesaurusId,
        boolean limitedOnThesaurus
) {
}
