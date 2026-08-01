package fr.cnrs.opentheso.v2.project.model;

import java.io.Serializable;

public record ProjectSummary(int id, String name) implements Serializable {

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
