package fr.cnrs.opentheso.v2.concept.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * État de progression partagé hors ViewScope, pour que le polling HTTP
 * lise les mêmes valeurs que le thread d'import.
 */
@Component
public class ThesaurusPickerImportProgressTracker {

    public static final int STEP_PREPARE = 0;
    public static final int STEP_CREATE = 1;
    public static final int STEP_CONCEPTS = 2;
    public static final int STEP_FINALIZE = 3;

    private static final long TTL_MS = 2L * 60L * 60L * 1000L;

    private final ConcurrentHashMap<String, ProgressState> states = new ConcurrentHashMap<>();

    public ProgressState start(String key) {
        purgeExpired();
        ProgressState state = new ProgressState();
        state.running = true;
        state.percent = 1;
        state.step = STEP_PREPARE;
        state.phase = "prepare";
        state.message = "";
        state.done = 0;
        state.total = 0;
        state.error = false;
        long now = System.currentTimeMillis();
        state.startedAtMs = now;
        state.updatedAtMs = now;
        state.conceptsStartedAtMs = 0L;
        states.put(key, state);
        return state;
    }

    public ProgressState get(String key) {
        return states.get(key);
    }

    public void update(String key, int percent, int step, String phase, String message, int done, int total) {
        ProgressState state = states.get(key);
        if (state == null) {
            return;
        }
        state.percent = Math.max(0, Math.min(100, percent));
        state.step = step;
        if (phase != null) {
            state.phase = phase;
            if ("concepts".equals(phase) && state.conceptsStartedAtMs <= 0L && done >= 0) {
                state.conceptsStartedAtMs = System.currentTimeMillis();
            }
        }
        if (message != null) {
            state.message = message;
        }
        if (done >= 0) {
            state.done = done;
        }
        if (total >= 0) {
            state.total = total;
        }
        state.updatedAtMs = System.currentTimeMillis();
    }

    public void succeed(String key) {
        ProgressState state = states.get(key);
        if (state == null) {
            return;
        }
        state.running = false;
        state.percent = 100;
        state.step = STEP_FINALIZE;
        state.phase = "done";
        state.updatedAtMs = System.currentTimeMillis();
    }

    public void fail(String key, String message) {
        ProgressState state = states.get(key);
        if (state == null) {
            return;
        }
        state.running = false;
        state.error = true;
        if (message != null) {
            state.message = message;
        }
        state.phase = "error";
        state.updatedAtMs = System.currentTimeMillis();
    }

    public void clear(String key) {
        states.remove(key);
    }

    public record ProgressSnapshot(
            boolean running,
            boolean error,
            int percent,
            int step,
            String phase,
            String message,
            int done,
            int total,
            long etaSeconds,
            double ratePerSec
    ) {
    }

    public ProgressSnapshot snapshot(String key) {
        purgeExpired();
        ProgressState state = states.get(key);
        if (state == null) {
            return new ProgressSnapshot(false, false, 0, 0, "idle", "", 0, 0, -1L, 0d);
        }
        double rate = 0d;
        long etaSeconds = -1L;
        if ("concepts".equals(state.phase) && state.done > 0 && state.conceptsStartedAtMs > 0L) {
            long elapsedMs = Math.max(1L, System.currentTimeMillis() - state.conceptsStartedAtMs);
            rate = state.done * 1000d / elapsedMs;
            if (rate > 0d && state.total > state.done) {
                etaSeconds = Math.max(1L, Math.round((state.total - state.done) / rate));
            } else if (state.total > 0 && state.done >= state.total) {
                etaSeconds = 0L;
            }
        }
        return new ProgressSnapshot(
                state.running,
                state.error,
                state.percent,
                state.step,
                state.phase,
                state.message,
                state.done,
                state.total,
                etaSeconds,
                rate
        );
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        states.entrySet().removeIf(entry -> {
            ProgressState state = entry.getValue();
            long ref = state.updatedAtMs > 0L ? state.updatedAtMs : state.startedAtMs;
            return ref > 0L && (now - ref) > TTL_MS;
        });
    }

    static final class ProgressState {
        volatile boolean running;
        volatile boolean error;
        volatile int percent;
        volatile int step;
        volatile String phase = "idle";
        volatile String message = "";
        volatile int done;
        volatile int total;
        volatile long startedAtMs;
        volatile long conceptsStartedAtMs;
        volatile long updatedAtMs;
    }
}
