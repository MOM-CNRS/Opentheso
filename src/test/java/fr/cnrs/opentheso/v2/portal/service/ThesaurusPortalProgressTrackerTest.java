package fr.cnrs.opentheso.v2.portal.service;

import fr.cnrs.opentheso.v2.portal.service.ThesaurusPortalProgressTracker.ProgressState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusPortalProgressTrackerTest {

    @Test
    void start_exposesSharedProgressOutsideViewScope() {
        ThesaurusPortalProgressTracker tracker = new ThesaurusPortalProgressTracker();

        ProgressState state = tracker.start("TH1-1");

        assertTrue(state.isRunning());
        assertTrue(state.isProgressVisible());
        assertEquals(4, state.getProgressValue());
        assertEquals("v2.portal.progress.prepare", state.getStatusMessage());
        assertEquals(state, tracker.get("TH1-1"));

        state.update(70, "v2.portal.progress.submit");
        assertEquals(70, tracker.get("TH1-1").getProgressValue());
        assertEquals("v2.portal.progress.submit", tracker.get("TH1-1").getStatusMessage());

        tracker.clear("TH1-1");
        assertNull(tracker.get("TH1-1"));
        assertFalse(state.isCompletionNotified());
    }
}
