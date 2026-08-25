package fr.cnrs.opentheso.v2.toolbox.actions.model;

import fr.cnrs.opentheso.v2.toolbox.model.LocalArkSettings;

import java.io.Serializable;

public class ActionsLotArkGenerateState implements Serializable {

    private String naan = "";
    private String prefix = "";
    private boolean overwrite;
    private boolean overwriteLocal;
    private boolean busy;
    private boolean done;
    private String globalError;
    private String message;
    private int appliedCount;
    private LocalArkSettings localSettings = new LocalArkSettings("", "", 0);

    public void resetResult() {
        done = false;
        globalError = null;
        message = null;
        appliedCount = 0;
    }

    public void applyResult(ActionsLotApplyResult result) {
        if (result.success()) {
            done = true;
            globalError = null;
            message = result.message();
            appliedCount = result.applied();
        } else {
            done = false;
            globalError = result.message();
            message = result.message();
        }
    }

    public String getCssClasses() {
        StringBuilder sb = new StringBuilder();
        if (done) {
            sb.append(" is-done");
        }
        if (busy) {
            sb.append(" is-busy");
        }
        if (globalError != null) {
            sb.append(" has-errors");
        }
        return sb.toString().trim();
    }

    public String getNaan() {
        return naan;
    }

    public void setNaan(String naan) {
        this.naan = naan;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean isOverwriteLocal() {
        return overwriteLocal;
    }

    public void setOverwriteLocal(boolean overwriteLocal) {
        this.overwriteLocal = overwriteLocal;
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public boolean isDone() {
        return done;
    }

    public String getGlobalError() {
        return globalError;
    }

    public void setGlobalError(String globalError) {
        this.globalError = globalError;
    }

    public String getMessage() {
        return message;
    }

    public int getAppliedCount() {
        return appliedCount;
    }

    public LocalArkSettings getLocalSettings() {
        return localSettings;
    }

    public void setLocalSettings(LocalArkSettings localSettings) {
        this.localSettings = localSettings == null ? new LocalArkSettings("", "", 0) : localSettings;
    }
}
