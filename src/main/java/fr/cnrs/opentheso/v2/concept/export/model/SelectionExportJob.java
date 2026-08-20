package fr.cnrs.opentheso.v2.concept.export.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

public class SelectionExportJob {

    static final int PHASE_COUNT = 4;
    private static final int[] PHASE_WEIGHTS = {12, 38, 35, 15};

    private volatile String status = "idle";
    private volatile int progress;
    private volatile int done;
    private volatile int total;
    private volatile String message = "";
    private volatile String filename;
    private volatile String contentType;
    private volatile String error;
    private volatile byte[] content;
    private volatile Path file;
    private volatile long bytes;
    private volatile long startedAt;
    private volatile boolean cancelRequested;
    private volatile int phaseIndex;
    private volatile String phase = "resolve";
    private volatile String phaseLabel = "Préparer";
    private Future<?> worker;

    public synchronized void reset() {
        worker = null;
        deleteFileQuietly();
        status = "idle";
        progress = 0;
        done = 0;
        total = 0;
        message = "";
        filename = null;
        contentType = null;
        error = null;
        content = null;
        bytes = 0;
        startedAt = 0;
        cancelRequested = false;
        phaseIndex = 0;
        phase = "resolve";
        phaseLabel = "Préparer";
    }

    public synchronized void start(int total, String message) {
        deleteFileQuietly();
        this.status = "running";
        this.error = null;
        this.content = null;
        this.filename = null;
        this.bytes = 0;
        this.startedAt = System.currentTimeMillis();
        enterPhase(0, "resolve", "Préparer", message);
        this.total = Math.max(0, total);
    }

    public synchronized void enterPhase(int index, String phase, String phaseLabel, String message) {
        this.status = "running";
        this.phaseIndex = Math.max(0, Math.min(PHASE_COUNT - 1, index));
        this.phase = phase == null ? "" : phase;
        this.phaseLabel = phaseLabel == null ? "" : phaseLabel;
        this.message = message == null ? this.phaseLabel : message;
        this.done = 0;
        this.total = 0;
        this.progress = Math.min(99, weightBefore(this.phaseIndex) + 1);
    }

    public synchronized void progress(int done, int total, String message) {
        this.done = Math.max(0, done);
        this.total = Math.max(0, total);
        if (message != null) {
            this.message = message;
        }
        int before = weightBefore(phaseIndex);
        int weight = PHASE_WEIGHTS[phaseIndex];
        int portion = this.total <= 0
                ? 1
                : (int) Math.round(this.done * (double) weight / this.total);
        this.progress = Math.min(99, Math.max(before + 1, before + portion));
    }

    public synchronized void complete(byte[] payload, String filename, String contentType, String message) {
        if (cancelRequested) {
            cancel();
            return;
        }
        if (!"running".equals(status)) {
            return;
        }
        deleteFileQuietly();
        byte[] data = payload == null ? new byte[0] : payload;
        this.status = "done";
        this.progress = 100;
        this.phaseIndex = PHASE_COUNT - 1;
        this.phase = "write";
        this.phaseLabel = "Fichier";
        this.done = Math.max(this.done, this.total);
        this.filename = filename;
        this.contentType = contentType;
        this.message = message;
        this.error = null;
        this.bytes = data.length;
        try {
            String suffix = extensionOf(filename);
            Path path = Files.createTempFile("ot-sel-export-", suffix);
            Files.write(path, data);
            this.file = path;
            this.content = null;
        } catch (IOException ex) {
            this.file = null;
            this.content = data;
        }
    }

    public synchronized void fail(String error) {
        if ("done".equals(status)) {
            return;
        }
        this.status = "error";
        this.error = error;
        this.message = error;
    }

    public synchronized void cancel() {
        this.status = "cancelled";
        this.message = "Export annulé";
        deleteFileQuietly();
        this.content = null;
        this.bytes = 0;
    }

    public synchronized void attachWorker(Future<?> worker) {
        this.worker = worker;
        if (cancelRequested && worker != null) {
            worker.cancel(true);
        }
    }

    public synchronized void clearWorker() {
        this.worker = null;
    }

    public void requestCancel() {
        Future<?> running;
        synchronized (this) {
            cancelRequested = true;
            running = worker;
        }
        if (running != null) {
            running.cancel(true);
        }
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public String getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    public int getDone() {
        return done;
    }

    public int getTotal() {
        return total;
    }

    public String getMessage() {
        return message;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getError() {
        return error;
    }

    public synchronized byte[] getContent() {
        if (content != null) {
            return content;
        }
        if (file != null && Files.isRegularFile(file)) {
            try {
                return Files.readAllBytes(file);
            } catch (IOException ex) {
                return null;
            }
        }
        return null;
    }

    public synchronized Path getFile() {
        return file;
    }

    public long getBytes() {
        return bytes;
    }

    public int getPhaseIndex() {
        return phaseIndex;
    }

    public SelectionExportStatus toStatus() {
        return new SelectionExportStatus(
                status,
                progress,
                done,
                total,
                message,
                filename,
                error,
                phase,
                phaseLabel,
                phaseIndex,
                PHASE_COUNT,
                bytes,
                "done".equals(status) && (content != null || file != null),
                startedAt
        );
    }

    private void deleteFileQuietly() {
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // best effort
            }
            file = null;
        }
        content = null;
        bytes = 0;
    }

    private static String extensionOf(String name) {
        if (name == null) {
            return ".bin";
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return ".bin";
        }
        String ext = name.substring(dot);
        return ext.length() > 8 ? ".bin" : ext;
    }

    private static int weightBefore(int index) {
        int sum = 0;
        for (int i = 0; i < index && i < PHASE_WEIGHTS.length; i++) {
            sum += PHASE_WEIGHTS[i];
        }
        return sum;
    }
}
