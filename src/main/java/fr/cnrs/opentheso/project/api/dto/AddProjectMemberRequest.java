package fr.cnrs.opentheso.project.api.dto;

import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(@NotNull Integer roleId) {
}
