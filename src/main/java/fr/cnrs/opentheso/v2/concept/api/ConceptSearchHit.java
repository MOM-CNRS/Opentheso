package fr.cnrs.opentheso.v2.concept.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConceptSearchHit(String id, String label, String type) {

    public ConceptSearchHit(String id, String label) {
        this(id, label, null);
    }
}
