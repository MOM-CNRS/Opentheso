package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public class MaintenanceArkEditor implements Serializable {

    private String naan = "";
    private String prefix = "";
    private boolean overwrite;
    private boolean overwriteLocalArk;

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

    public boolean isOverwriteLocalArk() {
        return overwriteLocalArk;
    }

    public void setOverwriteLocalArk(boolean overwriteLocalArk) {
        this.overwriteLocalArk = overwriteLocalArk;
    }
}
