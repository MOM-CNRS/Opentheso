package fr.cnrs.opentheso.v2.concept.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConceptAccessPolicyTest {

    @Test
    void hasSelectedThesaurus_requiresNonBlankId() {
        assertTrue(ConceptAccessPolicy.hasSelectedThesaurus("TH1"));
        assertFalse(ConceptAccessPolicy.hasSelectedThesaurus(""));
        assertFalse(ConceptAccessPolicy.hasSelectedThesaurus(null));
    }
}
