package fr.cnrs.opentheso.v2.sync.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncBatchResponseTest {

    @Test
    void from_aggregatesOutcomes() {
        SyncBatchResponse response = SyncBatchResponse.from(List.of(
                SyncConceptResult.skipped("C1", "C1", "ok"),
                SyncConceptResult.proposition("C2", "C2", 5),
                SyncConceptResult.candidate("C3", "CA3"),
                SyncConceptResult.error("C4", "boom")
        ));

        assertEquals(4, response.total());
        assertEquals(1, response.skipped());
        assertEquals(1, response.propositionsCreated());
        assertEquals(1, response.candidatesCreated());
        assertEquals(1, response.errors());
        assertEquals(4, response.results().size());
    }

    @Test
    void payloadBuilder_ignoresBlankValues() {
        SyncConceptPayload payload = SyncConceptPayload.builder()
                .identifier("C1")
                .prefLabel("fr", "Chat")
                .prefLabel("en", "  ")
                .altLabel("fr", "")
                .altLabel("fr", "Minou")
                .definition("fr", "Def")
                .build();

        assertEquals("Chat", payload.prefLabels().get("fr"));
        assertTrue(!payload.prefLabels().containsKey("en"));
        assertEquals(List.of("Minou"), payload.altLabels().get("fr"));
        assertEquals(List.of("Def"), payload.definitions().get("fr"));
    }
}
