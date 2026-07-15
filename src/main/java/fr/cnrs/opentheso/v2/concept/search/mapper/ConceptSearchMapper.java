package fr.cnrs.opentheso.v2.concept.search.mapper;

import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;

public final class ConceptSearchMapper {

    private ConceptSearchMapper() {
    }

    public static boolean isDeprecatedStatus(String status) {
        return ConceptStatusPolicy.isDeprecated(status);
    }
}
