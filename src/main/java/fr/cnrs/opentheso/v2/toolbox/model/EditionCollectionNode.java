package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EditionCollectionNode implements Serializable {

    private String id;
    private String label;
    private boolean privateCollection;
    private List<EditionCollectionNode> children = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isPrivateCollection() {
        return privateCollection;
    }

    public void setPrivateCollection(boolean privateCollection) {
        this.privateCollection = privateCollection;
    }

    public List<EditionCollectionNode> getChildren() {
        return children;
    }

    public void setChildren(List<EditionCollectionNode> children) {
        this.children = children;
    }
}
