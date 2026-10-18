package fr.cnrs.opentheso.models.propositions;

import fr.cnrs.opentheso.models.terms.NodeEM;
import lombok.Data;

@Data
public class SynonymPropBean extends NodeEM {

    private boolean toAdd;
    private boolean toRemove;
    private boolean toUpdate;
    private String idTerm;
}
