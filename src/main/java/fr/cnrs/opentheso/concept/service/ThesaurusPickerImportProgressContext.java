package fr.cnrs.opentheso.concept.service;

import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

/**
 * Propagation optionnelle de la progression d'import (SKOS / CSV)
 * sans changer les signatures des services existants.
 */
public final class ThesaurusPickerImportProgressContext {

    @FunctionalInterface
    public interface Sink {
        void accept(int done, int total, String phase);
    }

    private static final ThreadLocal<Sink> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<BooleanSupplier> CANCEL = new ThreadLocal<>();

    private ThesaurusPickerImportProgressContext() {
    }

    public static void runWith(Sink sink, Runnable work) {
        Sink previous = HOLDER.get();
        HOLDER.set(sink);
        try {
            work.run();
        } finally {
            if (previous != null) {
                HOLDER.set(previous);
            } else {
                HOLDER.remove();
            }
        }
    }

    public static <T> T callWith(Sink sink, Callable<T> work) throws Exception {
        return callWith(sink, null, work);
    }

    public static <T> T callWith(Sink sink, BooleanSupplier cancelled, Callable<T> work) throws Exception {
        Sink previous = HOLDER.get();
        BooleanSupplier previousCancel = CANCEL.get();
        HOLDER.set(sink);
        if (cancelled != null) {
            CANCEL.set(cancelled);
        } else {
            CANCEL.remove();
        }
        try {
            return work.call();
        } finally {
            if (previous != null) {
                HOLDER.set(previous);
            } else {
                HOLDER.remove();
            }
            if (previousCancel != null) {
                CANCEL.set(previousCancel);
            } else {
                CANCEL.remove();
            }
        }
    }

    /**
     * Interrompt le lot en cours si l'utilisateur a annulé l'import.
     */
    public static void ensureActive() {
        BooleanSupplier cancelled = CANCEL.get();
        if (cancelled != null && cancelled.getAsBoolean()) {
            throw new ImportCancelledException();
        }
    }

    public static void report(int done, int total, String phase) {
        Sink sink = HOLDER.get();
        if (sink != null) {
            // -1 = conserver la valeur précédente côté tracker (phase seule)
            int safeDone = done < 0 ? -1 : Math.max(0, done);
            int safeTotal = total < 0 ? -1 : Math.max(0, total);
            sink.accept(safeDone, safeTotal, phase);
        }
    }

    public static void reportPhase(String phase) {
        Sink sink = HOLDER.get();
        if (sink != null) {
            sink.accept(-1, -1, phase);
        }
    }
}
