package fr.cnrs.opentheso.v2.sync.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusSyncProgressTrackerTest {

    private ThesaurusSyncProgressTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ThesaurusSyncProgressTracker();
    }

    @Test
    void start_storesRunningVisibleStateWithInitialMessage() {
        var state = tracker.start("job-1");

        assertTrue(state.running);
        assertTrue(state.progressVisible);
        assertEquals(1, state.progressValue);
        assertEquals("Préparation de la synchronisation…", state.statusMessage);
        assertSame(state, tracker.get("job-1"));
    }

    @Test
    void get_returnsNullForUnknownKey() {
        assertNull(tracker.get("missing"));
    }

    @Test
    void finish_marksNotRunningButKeepsProgress() {
        var state = tracker.start("job-1");
        state.progressValue = 80;
        state.statusMessage = "Lot 1 envoyé";

        tracker.finish("job-1");

        assertFalse(state.running);
        assertEquals(80, tracker.get("job-1").progressValue);
        assertEquals("Lot 1 envoyé", tracker.get("job-1").statusMessage);
    }

    @Test
    void finish_unknownKey_isNoOp() {
        tracker.finish("missing");
        assertNull(tracker.get("missing"));
    }

    @Test
    void clear_removesState() {
        tracker.start("job-1");
        tracker.clear("job-1");
        assertNull(tracker.get("job-1"));
    }

    @Test
    void start_overwritesPreviousStateForSameKey() {
        tracker.start("job-1").progressValue = 50;
        var second = tracker.start("job-1");

        assertNotNull(second);
        assertEquals(1, second.progressValue);
        assertTrue(second.running);
    }
}
