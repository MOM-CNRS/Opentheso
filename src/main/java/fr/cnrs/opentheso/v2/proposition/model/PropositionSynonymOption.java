package fr.cnrs.opentheso.v2.proposition.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class PropositionSynonymOption implements Serializable {

    private String lang;
    private String value;
    private String oldValue;
    private boolean hidden;
    private boolean oldHidden;
    private boolean toAdd;
    private boolean toUpdate;
    private boolean toRemove;
}
