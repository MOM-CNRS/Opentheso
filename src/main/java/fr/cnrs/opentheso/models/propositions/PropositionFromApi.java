package fr.cnrs.opentheso.models.propositions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropositionFromApi implements Serializable {

    private String IdTheso;
    private String conceptID;
    private String commentaire;
    private List<SynonymPropBean> synonymsProp;
    private List<TraductionPropBean> traductionsProp;
    private List<NotePropBean> definitions, notes;
}
