package fr.cnrs.opentheso.v2.admin.api.dto;

import jakarta.validation.constraints.NotNull;

public record MoveAdminThesaurusRequest(@NotNull Integer targetProjectId) {
}
