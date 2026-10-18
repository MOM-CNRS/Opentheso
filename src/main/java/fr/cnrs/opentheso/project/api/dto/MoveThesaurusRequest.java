package fr.cnrs.opentheso.project.api.dto;

import jakarta.validation.constraints.NotNull;

public record MoveThesaurusRequest(@NotNull Integer targetProjectId) {
}
