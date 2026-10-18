package fr.cnrs.opentheso.user.api.dto;

import java.util.List;

public record ConceptBlockLayoutDto(List<String> order, List<String> collapsed) {
}
