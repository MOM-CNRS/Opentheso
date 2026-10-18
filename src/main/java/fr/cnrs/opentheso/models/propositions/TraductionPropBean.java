package fr.cnrs.opentheso.models.propositions;

import fr.cnrs.opentheso.models.terms.NodeTermTraduction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TraductionPropBean extends NodeTermTraduction {

    private String oldValue;
    private String idTerm;
    private boolean toAdd;
    private boolean toRemove;
    private boolean toUpdate;
}
