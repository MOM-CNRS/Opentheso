package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public record ProjectOption(
        int id,
        String name
) implements Serializable {

    public int getId() {
        return id;
    }

    public String getIdAsString() {
        return String.valueOf(id);
    }

    /** Accesseur EL record ({@code proj.idAsString()}). */
    public String idAsString() {
        return String.valueOf(id);
    }

    public String getName() {
        return name;
    }
}
