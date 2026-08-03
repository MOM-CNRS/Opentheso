package fr.cnrs.opentheso.v2.sync.service;

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
        state.running = true;
        state.progressVisible = true;
        state.progressValue = 1;
        state.statusMessage = "Préparation de la synchronisation…";
        states.put(key, state);
        return state;
    }

    public ProgressState get(String key) {
        return states.get(key);
    }

    public void finish(String key) {
        ProgressState state = states.get(key);
        if (state != null) {
            state.running = false;
        }
    }

    public void clear(String key) {
        states.remove(key);
    }

    public static final class ProgressState {
        public volatile boolean running;
        public volatile boolean progressVisible;
        public volatile int progressValue;
        public volatile int processed;
        public volatile int total;
        public volatile int skipped;
        public volatile int propositions;
        public volatile int candidates;
        public volatile int errors;
        public volatile String statusMessage = "";
        public volatile boolean lastSyncFailed;
        public volatile String lastSyncError;
        public volatile boolean completionNotified;
    }
}
