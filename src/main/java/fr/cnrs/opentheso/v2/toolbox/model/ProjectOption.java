package fr.cnrs.opentheso.v2.toolbox.model;

public record ProjectOption(
        int id,
        String name
) {

    public int getId() {
        return id;
    }

    public String getIdAsString() {
        return String.valueOf(id);
    }

    public String getName() {
        return name;
    }
}
