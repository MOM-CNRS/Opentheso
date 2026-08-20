package fr.cnrs.opentheso.v2.concept.export.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectionExportJobTest {

    @Test
    void enterPhase_weightsProgressAcrossFourSteps() {
        var job = new SelectionExportJob();
        job.enterPhase(0, "resolve", "Préparer", "Préparation…");
        assertEquals(1, job.getProgress());
        assertEquals("Préparer", job.toStatus().phaseLabel());

        job.enterPhase(1, "read", "Lire", "Lecture…");
        job.progress(0, 348, "Chargement de 348 concepts en base…");
        assertEquals(13, job.getProgress());
        assertEquals(0, job.getDone());
        assertEquals(348, job.getTotal());
        assertEquals("Lire", job.toStatus().phaseLabel());

        job.enterPhase(2, "build", "Construire", "Construction…");
        job.progress(50, 100, "Concept 50 / 100 · C50");
        assertEquals(68, job.getProgress());
        assertEquals(2, job.getPhaseIndex());
        assertEquals("Concept 50 / 100 · C50", job.getMessage());

        job.enterPhase(3, "write", "Fichier", "Écriture…");
        assertEquals(86, job.getProgress());

        job.complete(new byte[]{1}, "a.rdf", "application/rdf+xml", "Fichier RDF/XML prêt · 1 concept");
        assertEquals(100, job.getProgress());
        assertEquals("done", job.getStatus());
        assertEquals(3, job.toStatus().phaseIndex());
        assertEquals(4, job.toStatus().phaseCount());
        assertEquals("Fichier RDF/XML prêt · 1 concept", job.toStatus().message());
        assertEquals(1, job.toStatus().bytes());
        assertTrue(job.toStatus().downloadable());
        job.reset();
        assertEquals("idle", job.getStatus());
        assertFalse(job.toStatus().downloadable());
    }

    @Test
    void requestCancel_interruptsAttachedWorker() throws Exception {
        var job = new SelectionExportJob();
        var started = new java.util.concurrent.CountDownLatch(1);
        var finished = new java.util.concurrent.CountDownLatch(1);
        var interrupted = new java.util.concurrent.atomic.AtomicBoolean();
        var pool = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var future = pool.submit(() -> {
                started.countDown();
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException ex) {
                    interrupted.set(true);
                } finally {
                    finished.countDown();
                }
            });
            job.attachWorker(future);
            assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS));
            job.requestCancel();
            assertTrue(job.isCancelRequested());
            assertTrue(finished.await(2, java.util.concurrent.TimeUnit.SECONDS));
            assertTrue(interrupted.get() || future.isCancelled());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void complete_isIgnoredWhenCancelWasRequested() {
        var job = new SelectionExportJob();
        job.start(1, "run");
        job.requestCancel();
        job.complete(new byte[]{1, 2}, "a.rdf", "application/rdf+xml", "prêt");
        assertEquals("cancelled", job.getStatus());
        assertFalse(job.toStatus().downloadable());
    }
}
