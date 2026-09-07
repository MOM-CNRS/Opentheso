package fr.cnrs.opentheso.v2.sync.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * État de progression partagé hors ViewScope, pour que le {@code p:poll}
 * lise toujours les mêmes valeurs que le thread de synchronisation.
 */
@Component
public class ThesaurusSyncProgressTracker {

    private final ConcurrentHashMap<String, ProgressState> states = new ConcurrentHashMap<>();

    public ProgressState start(String key) {
        ProgressState state = new ProgressState();
        state.setRunning(true);
        state.setProgressVisible(true);
        state.setProgressValue(1);
        state.setStatusMessage("Préparation de la synchronisation…");
        states.put(key, state);
        return state;
    }

    public ProgressState get(String key) {
        return states.get(key);
    }

    public void finish(String key) {
        ProgressState state = states.get(key);
        if (state != null) {
            state.setRunning(false);
        }
    }

    public void clear(String key) {
        states.remove(key);
    }

    @Getter
    @Setter
    public static final class ProgressState {
        private volatile boolean running;
        private volatile boolean progressVisible;
        private volatile int progressValue;
        private volatile int processed;
        private volatile int total;
        private volatile int skipped;
        private volatile int propositions;
        private volatile int candidates;
        private volatile int errors;
        private volatile String statusMessage = "";
        private volatile boolean lastSyncFailed;
        private volatile String lastSyncError;
        private volatile boolean completionNotified;
    }
}
