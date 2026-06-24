package fr.cnrs.opentheso.v2.project.api.dto;

import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(@NotNull Integer roleId) {
}
