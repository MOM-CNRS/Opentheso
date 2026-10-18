package fr.cnrs.opentheso.concept.search.mapper;

import fr.cnrs.opentheso.concept.policy.ConceptStatusPolicy;

public final class ConceptSearchMapper {

    private ConceptSearchMapper() {
    }

    public static boolean isDeprecatedStatus(String status) {
        return ConceptStatusPolicy.isDeprecated(status);
    }
}
