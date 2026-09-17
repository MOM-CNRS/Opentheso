package fr.cnrs.opentheso.v2.concept.service;

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

    public static <T> T callWith(Sink sink, java.util.concurrent.Callable<T> work) throws Exception {
        Sink previous = HOLDER.get();
        HOLDER.set(sink);
        try {
            return work.call();
        } finally {
            if (previous != null) {
                HOLDER.set(previous);
            } else {
                HOLDER.remove();
            }
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
