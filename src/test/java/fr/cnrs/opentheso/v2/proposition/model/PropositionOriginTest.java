package fr.cnrs.opentheso.v2.proposition.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropositionOriginTest {

    @Test
    void storedFlagWins() {
        assertTrue(PropositionOrigin.fromSync(true, "demande manuelle"));
        assertTrue(PropositionOrigin.fromSync(Boolean.TRUE, null));
    }

    @Test
    void legacySyncCommentIsDetectedWhenFlagIsMissing() {
        assertTrue(PropositionOrigin.fromSync(null, "Synchronisation depuis le thésaurus copie"));
        assertTrue(PropositionOrigin.fromSync(false, "synchronisation depuis le thésaurus maître"));
    }

    @Test
    void manualCommentIsNotSync() {
        assertFalse(PropositionOrigin.fromSync(null, "Merci d'ajouter un synonyme"));
        assertFalse(PropositionOrigin.fromSync(false, "  "));
        assertFalse(PropositionOrigin.fromSync(null, null));
    }
}
