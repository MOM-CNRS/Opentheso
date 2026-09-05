package fr.cnrs.opentheso.v2.toolbox.edition.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ThesaurusCsvConceptLabel implements Serializable {

    private String label;
    private String lang;
}
