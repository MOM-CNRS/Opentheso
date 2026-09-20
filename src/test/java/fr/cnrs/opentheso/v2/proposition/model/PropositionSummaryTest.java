package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropositionSummaryTest {

    @Test
    void envoyer_isExposedAsRecordStyleAccessorForJsEl() {
        var summary = summary("ENVOYER");

        assertTrue(summary.envoyer());
        assertTrue(summary.isEnvoyer());
        assertTrue(summary.pending());
        assertEquals("is-new", summary.statusCss());
        assertFalse(summary.lu());
        assertFalse(summary.approuver());
        assertFalse(summary.refuser());
    }

    @Test
    void lu_isPendingButNotNew() {
        var summary = summary("LU");

        assertTrue(summary.lu());
        assertTrue(summary.isLu());
        assertTrue(summary.pending());
        assertEquals("is-read", summary.statusCss());
        assertFalse(summary.envoyer());
    }

    @Test
    void approuverAndRefuser_areTerminalStatuses() {
        var approved = summary("APPROUVER");
        var refused = summary("REFUSER");

        assertTrue(approved.approuver());
        assertTrue(approved.isApprouver());
        assertEquals("is-approved", approved.statusCss());
        assertFalse(approved.pending());

        assertTrue(refused.refuser());
        assertTrue(refused.isRefuser());
        assertEquals("is-refused", refused.statusCss());
        assertFalse(refused.pending());
    }

    private static PropositionSummary summary(String status) {
        return new PropositionSummary(
                1, "TH1", "C1", "Label", "alice", "a@x.fr", status, "01-01-2024", "fr", "fr");
    }
}
