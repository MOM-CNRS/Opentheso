package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropositionAcceptanceTest {

    @Test
    void none_rejectsAllCategories() {
        var acceptance = PropositionAcceptance.none();

        for (PropositionFieldCategory category : PropositionFieldCategory.values()) {
            assertFalse(acceptance.isAccepted(category), category + " should not be accepted");
        }
    }

    @Test
    void isAccepted_mapsEachCategoryToItsOwnFlag() {
        assertTrue(new PropositionAcceptance(true, false, false, false, false, false, false, false, false, false)
                .isAccepted(PropositionFieldCategory.NOM));
        assertTrue(new PropositionAcceptance(false, true, false, false, false, false, false, false, false, false)
                .isAccepted(PropositionFieldCategory.SYNONYME));
        assertTrue(new PropositionAcceptance(false, false, true, false, false, false, false, false, false, false)
                .isAccepted(PropositionFieldCategory.TRADUCTION));
        assertTrue(new PropositionAcceptance(false, false, false, true, false, false, false, false, false, false)
                .isAccepted(PropositionFieldCategory.NOTE));
        assertTrue(new PropositionAcceptance(false, false, false, false, true, false, false, false, false, false)
                .isAccepted(PropositionFieldCategory.DEFINITION));
        assertTrue(new PropositionAcceptance(false, false, false, false, false, true, false, false, false, false)
                .isAccepted(PropositionFieldCategory.CHANGE_NOTE));
        assertTrue(new PropositionAcceptance(false, false, false, false, false, false, true, false, false, false)
                .isAccepted(PropositionFieldCategory.SCOPE));
        assertTrue(new PropositionAcceptance(false, false, false, false, false, false, false, true, false, false)
                .isAccepted(PropositionFieldCategory.EDITORIAL_NOTE));
        assertTrue(new PropositionAcceptance(false, false, false, false, false, false, false, false, true, false)
                .isAccepted(PropositionFieldCategory.EXAMPLE));
        assertTrue(new PropositionAcceptance(false, false, false, false, false, false, false, false, false, true)
                .isAccepted(PropositionFieldCategory.HISTORY));
    }

    @Test
    void isAccepted_doesNotConfuseUnrelatedFlags() {
        var acceptance = new PropositionAcceptance(true, false, false, false, false, false, false, false, false, false);

        assertFalse(acceptance.isAccepted(PropositionFieldCategory.SYNONYME));
        assertFalse(acceptance.isAccepted(PropositionFieldCategory.NOTE));
    }
}
