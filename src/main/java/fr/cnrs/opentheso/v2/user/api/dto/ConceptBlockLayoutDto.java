package fr.cnrs.opentheso.v2.user.api.dto;

import java.util.List;

public record ConceptBlockLayoutDto(List<String> order, List<String> collapsed) {
}
