package fr.cnrs.opentheso.v2.sync.model;

import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncSendService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncModelTest {

    @Test
    void syncConceptResult_factoriesSetOutcomeAndFields() {
        assertEquals(SyncConceptOutcome.SKIPPED, SyncConceptResult.skipped("C1", "M1", "ok").outcome());
        assertEquals("ok", SyncConceptResult.skipped("C1", "M1", "ok").message());

        SyncConceptResult proposition = SyncConceptResult.proposition("C2", "M2", 9);
        assertEquals(SyncConceptOutcome.PROPOSITION_CREATED, proposition.outcome());
        assertEquals(9, proposition.propositionId());

        SyncConceptResult candidate = SyncConceptResult.candidate("C3", "CA3");
        assertEquals(SyncConceptOutcome.CANDIDATE_CREATED, candidate.outcome());
        assertEquals("CA3", candidate.matchedConceptId());

        SyncConceptResult error = SyncConceptResult.error("C4", "boom");
        assertEquals(SyncConceptOutcome.ERROR, error.outcome());
        assertEquals("boom", error.message());
        assertNull(error.propositionId());
    }

    @Test
    void syncProgress_percentHandlesEdgeCases() {
        assertEquals(0, new ThesaurusSyncSendService.SyncProgress(0, 0, 0, 0, 0, 0, "").percent());
        assertEquals(0, new ThesaurusSyncSendService.SyncProgress(-1, 1, 0, 0, 0, 0, "").percent());
        assertEquals(33, new ThesaurusSyncSendService.SyncProgress(3, 1, 0, 0, 0, 0, "").percent());
        assertEquals(100, new ThesaurusSyncSendService.SyncProgress(2, 3, 0, 0, 0, 0, "").percent());
    }

    @Test
    void syncBatchResponse_fromEmptyList() {
        SyncBatchResponse response = SyncBatchResponse.from(List.of());
        assertEquals(0, response.total());
        assertEquals(0, response.skipped());
        assertTrue(response.results().isEmpty());
    }

    @Test
    void syncConceptPayload_nullMapsBecomeEmptyImmutable() {
        SyncConceptPayload payload = new SyncConceptPayload(
                "C1", null, null, null, null, null, null, null);

        assertTrue(payload.prefLabels().isEmpty());
        assertTrue(payload.altLabels().isEmpty());
        assertTrue(payload.notes().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> payload.prefLabels().put("fr", "x"));
    }

    @Test
    void syncConceptPayload_copiesIncomingMaps() {
        Map<String, String> prefs = new HashMap<>();
        prefs.put("fr", "Chat");
        SyncConceptPayload payload = new SyncConceptPayload(
                "C1", "ark", "n1", prefs, Map.of(), Map.of(), Map.of(), Map.of());

        prefs.put("fr", "Changed");
        assertEquals("Chat", payload.prefLabels().get("fr"));
    }
}
