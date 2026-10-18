package fr.cnrs.opentheso.models.propositions;

import fr.cnrs.opentheso.models.notes.NodeNote;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotePropBean extends NodeNote {

    private String oldValue;
    private boolean toAdd;
    private boolean toRemove;
    private boolean toUpdate;
}
