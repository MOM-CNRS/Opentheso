package fr.cnrs.opentheso.models.alignment;

import lombok.Data;

import java.io.Serializable;


@Data
public class SelectedResource implements Serializable {

    private static final long serialVersionUID = 1L;

    private String idLang;
    private String gettedValue;
    private boolean selected = true;
    private String localValue;
    private boolean isEqual;
    
}
