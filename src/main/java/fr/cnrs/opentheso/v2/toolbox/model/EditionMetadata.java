package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public class EditionMetadata implements Serializable {

    private int id = -1;
    private String name = "";
    private String value = "";
    private String language = "";
    private String type = "";

    public static EditionMetadata emptyRow() {
        return new EditionMetadata();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
