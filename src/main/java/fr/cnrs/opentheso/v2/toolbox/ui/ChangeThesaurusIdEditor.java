package fr.cnrs.opentheso.v2.toolbox.ui;

import java.io.Serializable;

public class ChangeThesaurusIdEditor implements Serializable {

    private String currentId = "";
    private String newId = "";

    public static ChangeThesaurusIdEditor empty() {
        return new ChangeThesaurusIdEditor();
    }

    public static ChangeThesaurusIdEditor forThesaurus(String currentId) {
        var editor = new ChangeThesaurusIdEditor();
        editor.currentId = currentId;
        return editor;
    }

    public String getCurrentId() {
        return currentId;
    }

    public String getNewId() {
        return newId;
    }

    public void setNewId(String newId) {
        this.newId = newId;
    }
}
