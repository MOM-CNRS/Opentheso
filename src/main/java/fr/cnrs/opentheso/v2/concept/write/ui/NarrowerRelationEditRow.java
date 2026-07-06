package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class NarrowerRelationEditRow implements Serializable {

    private String conceptId;
    private String label;
    private String role;

    public NarrowerRelationEditRow() {
    }

    public NarrowerRelationEditRow(String conceptId, String label, String role) {
        this.conceptId = conceptId;
        this.label = label;
        this.role = role;
    }
}
