package fr.cnrs.opentheso.v2.user.model;

import java.util.List;
import java.io.Serializable;
import java.util.Set;

public record ConceptBlockLayout(List<String> order, Set<String> collapsed) implements Serializable {

    public static ConceptBlockLayout defaults() {
        return new ConceptBlockLayout(ConceptBlockIds.DEFAULT_ORDER, Set.of());
    }
}
