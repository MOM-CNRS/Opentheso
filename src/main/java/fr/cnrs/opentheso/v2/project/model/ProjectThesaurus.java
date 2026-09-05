package fr.cnrs.opentheso.v2.project.model;

import java.io.Serializable;

public record ProjectThesaurus(String id, String title, boolean privateThesaurus) implements Serializable {
}
