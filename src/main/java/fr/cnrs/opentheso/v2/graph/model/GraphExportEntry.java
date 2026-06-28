package fr.cnrs.opentheso.v2.graph.model;

import java.io.Serializable;

public record GraphExportEntry(String thesaurusId, String conceptId) implements Serializable {
}
