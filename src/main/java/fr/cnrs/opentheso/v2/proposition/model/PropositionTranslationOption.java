package fr.cnrs.opentheso.v2.proposition.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class PropositionTranslationOption implements Serializable {

    private String lang;
    private String value;
    private String oldValue;
    private boolean toAdd;
    private boolean toUpdate;
    private boolean toRemove;
}
