package fr.cnrs.opentheso.v2.portal.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Progression partagée hors ViewScope, pour que le poll JSF lise
 * les mêmes valeurs que le thread de publication.
 */
@Component
public class ThesaurusPortalProgressTracker {

    private final ConcurrentHashMap<String, ProgressState> states = new ConcurrentHashMap<>();

    public ProgressState start(String key) {
        return start(key, "v2.portal.progress.prepare");
    }

    public ProgressState start(String key, String message) {
        ProgressState state = new ProgressState();
        state.setRunning(true);
        state.setProgressVisible(true);
        state.setProgressValue(4);
        state.setStatusMessage(message == null || message.isBlank()
                ? "v2.portal.progress.prepare" : message);
        states.put(key, state);
        return state;
    }

    public ProgressState get(String key) {
        return states.get(key);
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
        private volatile String statusMessage = "";
        private volatile boolean failed;
        private volatile String error;
        private volatile boolean created;
        private volatile String ontologyUrl;
        private volatile String pullLocation;
        private volatile boolean removed;
        private volatile boolean completionNotified;

        public void update(int percent, String message) {
            progressValue = Math.max(0, Math.min(100, percent));
            if (message != null) {
                statusMessage = message;
            }
        }
    }
}
