package fr.cnrs.opentheso.v2.admin.model;

import java.io.Serializable;

public record AdminProject(
        int id,
        String name,
        int thesaurusCount
) implements Serializable {

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getThesaurusCount() {
        return thesaurusCount;
    }
}
