package fr.cnrs.opentheso.v2.concept.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptStatusPolicyTest {

    @Test
    void isDeprecated_matchesDepStatusOnly() {
        assertTrue(ConceptStatusPolicy.isDeprecated("dep"));
        assertTrue(ConceptStatusPolicy.isDeprecated("DEP"));
        assertFalse(ConceptStatusPolicy.isDeprecated("D")); // "D" = active/published in OpenTheso schema (DEFAULT status)
        assertFalse(ConceptStatusPolicy.isDeprecated("C"));
        assertFalse(ConceptStatusPolicy.isDeprecated("CA"));
        assertFalse(ConceptStatusPolicy.isDeprecated(null));
        assertFalse(ConceptStatusPolicy.isDeprecated(""));
    }
}
