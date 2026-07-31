package fr.cnrs.opentheso.v2.concept.alignment.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlignmentSourceItem {

    private int sourceId;
    private String label;
    private String description;
    private boolean selected;
    private boolean global;
}
