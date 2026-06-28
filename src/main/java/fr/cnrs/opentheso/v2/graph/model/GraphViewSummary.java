package fr.cnrs.opentheso.v2.graph.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GraphViewSummary implements Serializable {

    private int id;
    private String name;
    private String description;
    private List<GraphExportEntry> exports = new ArrayList<>();

    public GraphViewSummary() {
    }

    public GraphViewSummary(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<GraphExportEntry> getExports() {
        return exports;
    }

    public void setExports(List<GraphExportEntry> exports) {
        this.exports = exports == null ? new ArrayList<>() : exports;
    }

    public boolean hasExports() {
        return exports != null && !exports.isEmpty();
    }
}
