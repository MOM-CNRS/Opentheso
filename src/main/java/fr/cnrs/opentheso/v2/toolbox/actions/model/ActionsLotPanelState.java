package fr.cnrs.opentheso.v2.toolbox.actions.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * État UI d'un panneau Importer / Supprimer (alignements).
 */
public class ActionsLotPanelState implements Serializable {

    private String identifierType = "identifier";
    private int choiceDelimiter;
    private String fileName;
    private String fileMeta;
    private byte[] fileBytes;
    private boolean hasFile;
    private boolean checked;
    private boolean done;
    private boolean busy;
    private String globalError;

    private int linesRead;
    private int validCount;
    private int errorCount;
    private int ignoredCount;
    private int appliedCount;
    private int rejectedCount;

    private List<ActionsLotLineError> errors = new ArrayList<>();
    private List<ActionsLotAlignmentCandidate> validCandidates = new ArrayList<>();

    public void resetFile() {
        fileName = null;
        fileMeta = null;
        fileBytes = null;
        hasFile = false;
        checked = false;
        done = false;
        busy = false;
        globalError = null;
        linesRead = 0;
        validCount = 0;
        errorCount = 0;
        ignoredCount = 0;
        appliedCount = 0;
        rejectedCount = 0;
        errors = new ArrayList<>();
        validCandidates = new ArrayList<>();
    }

    public void acceptFile(String name, byte[] bytes) {
        resetFile();
        fileName = name;
        fileBytes = bytes;
        hasFile = bytes != null && bytes.length > 0;
        long sizeKo = hasFile ? Math.max(1, Math.round(bytes.length / 1024.0)) : 0;
        fileMeta = hasFile ? sizeKo + " Ko" : null;
    }

    public void applyValidation(ActionsLotValidationResult result) {
        checked = true;
        done = false;
        if (!result.success()) {
            globalError = result.errorMessage();
            linesRead = 0;
            validCount = 0;
            errorCount = 0;
            ignoredCount = 0;
            errors = new ArrayList<>();
            validCandidates = new ArrayList<>();
            return;
        }
        globalError = null;
        linesRead = result.linesRead();
        validCount = result.validCount();
        errorCount = result.errorCount();
        ignoredCount = result.ignoredCount();
        errors = new ArrayList<>(result.errors() != null ? result.errors() : List.of());
        validCandidates = new ArrayList<>(result.validCandidates() != null ? result.validCandidates() : List.of());
        if (fileMeta != null && !fileMeta.contains("ligne")) {
            fileMeta = linesRead + " ligne" + (linesRead > 1 ? "s" : "") + " · " + fileMeta;
        }
    }

    public void applyResult(ActionsLotApplyResult result) {
        done = result.success();
        appliedCount = result.applied();
        rejectedCount = result.rejected();
        if (!result.success()) {
            globalError = result.message();
        }
    }

    public String getCssClasses() {
        StringBuilder sb = new StringBuilder();
        if (hasFile) {
            sb.append(" has-file");
        }
        if (checked) {
            sb.append(" is-checked");
        }
        if (checked && errorCount == 0 && globalError == null) {
            sb.append(" is-corrected");
        }
        if (done) {
            sb.append(" is-done");
        }
        if (busy) {
            sb.append(" is-busy");
        }
        if (globalError != null || (checked && errorCount > 0)) {
            sb.append(" has-errors");
        }
        return sb.toString().trim();
    }

    public boolean isCorrected() {
        return checked && errorCount == 0 && globalError == null;
    }

    public List<ActionsLotLineError> getErrors() {
        return ActionsLotErrorView.limit(errors);
    }

    public List<ActionsLotAlignmentCandidate> getValidCandidates() {
        return validCandidates == null ? Collections.emptyList() : validCandidates;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public int getChoiceDelimiter() {
        return choiceDelimiter;
    }

    public void setChoiceDelimiter(int choiceDelimiter) {
        this.choiceDelimiter = choiceDelimiter;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileMeta() {
        return fileMeta;
    }

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public boolean isHasFile() {
        return hasFile;
    }

    public boolean isChecked() {
        return checked;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public String getGlobalError() {
        return globalError;
    }

    public void setGlobalError(String globalError) {
        this.globalError = globalError;
    }

    public int getLinesRead() {
        return linesRead;
    }

    public int getValidCount() {
        return validCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getIgnoredCount() {
        return ignoredCount;
    }

    public int getAppliedCount() {
        return appliedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }
}
