package fr.cnrs.opentheso.v2.concept.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusPickerImportProgressTrackerTest {

    @Test
    void snapshot_computesEtaAndRateDuringConceptsPhase() throws Exception {
        ThesaurusPickerImportProgressTracker tracker = new ThesaurusPickerImportProgressTracker();
        tracker.start("k1");
        tracker.update("k1", 20, ThesaurusPickerImportProgressTracker.STEP_CONCEPTS, "concepts",
                "Concept 50 / 200", 50, 200);
        Thread.sleep(50);
        tracker.update("k1", 40, ThesaurusPickerImportProgressTracker.STEP_CONCEPTS, "concepts",
                "Concept 100 / 200", 100, 200);

        var snap = tracker.snapshot("k1");
        assertEquals(100, snap.done());
        assertEquals(200, snap.total());
        assertTrue(snap.ratePerSec() > 0d);
        assertTrue(snap.etaSeconds() > 0L);
    }

    @Test
    void snapshot_idleWhenUnknownKey() {
        ThesaurusPickerImportProgressTracker tracker = new ThesaurusPickerImportProgressTracker();
        var snap = tracker.snapshot("missing");
        assertEquals("idle", snap.phase());
        assertEquals(-1L, snap.etaSeconds());
    }

    @Test
    void start_purgesEntriesOlderThanTtl() {
        ThesaurusPickerImportProgressTracker tracker = new ThesaurusPickerImportProgressTracker();
        tracker.start("old");
        var oldState = tracker.get("old");
        oldState.startedAtMs = System.currentTimeMillis() - (3L * 60L * 60L * 1000L);
        oldState.updatedAtMs = oldState.startedAtMs;

        tracker.start("fresh");

        org.junit.jupiter.api.Assertions.assertNull(tracker.get("old"));
        org.junit.jupiter.api.Assertions.assertNotNull(tracker.get("fresh"));
    }
}
