package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public class LocalArkSettings implements Serializable {

    private String naan = "";
    private String prefix = "";
    private int size;

    public LocalArkSettings() {
    }

    public LocalArkSettings(String naan, String prefix, int size) {
        this.naan = naan;
        this.prefix = prefix;
        this.size = size;
    }

    public String getNaan() {
        return naan;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getSize() {
        return size;
    }
}
