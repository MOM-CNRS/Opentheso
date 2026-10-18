package fr.cnrs.opentheso.graph.model;

import java.io.Serializable;

public record GraphExportEntry(String thesaurusId, String conceptId) implements Serializable {
}
