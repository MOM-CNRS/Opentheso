package fr.cnrs.opentheso.project.model;

import java.io.Serializable;

public record ProjectThesaurus(String id, String title, boolean privateThesaurus) implements Serializable {
}
