package fr.cnrs.opentheso.user.ui;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConceptBlockLayoutRow implements Serializable {

    private String id;
    private String label;
    private boolean open;

    public String getStateLabelKey() {
        return open ? "profile.blocks.open" : "profile.blocks.closed";
    }
}
