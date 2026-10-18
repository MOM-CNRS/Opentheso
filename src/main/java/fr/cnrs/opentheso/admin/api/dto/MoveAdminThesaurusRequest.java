package fr.cnrs.opentheso.admin.api.dto;

import jakarta.validation.constraints.NotNull;

public record MoveAdminThesaurusRequest(@NotNull Integer targetProjectId) {
}
