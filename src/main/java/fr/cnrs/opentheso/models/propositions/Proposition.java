package fr.cnrs.opentheso.models.propositions;

import fr.cnrs.opentheso.models.terms.Term;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Data
public class Proposition implements Serializable {

    private String conceptID;

    private boolean updateNomConcept;
    private String nomConceptProp;
    private Term nomConcept;

    private List<SynonymPropBean> synonymsProp;
    private List<TraductionPropBean> traductionsProp;

    private NotePropBean note, scopeNote, historyNote, example,
            editorialNote, definition, changeNote;

    
    public Proposition() {
        synonymsProp = new ArrayList<>();
        traductionsProp = new ArrayList<>();
        note = null;
        scopeNote = null;
        historyNote = null;
        example = null;
        editorialNote = null;
        definition = null;
        changeNote = null;
    }
}
