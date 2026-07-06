package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SynonymEditRow implements Serializable {

    private String lang;
    private String oldValue;
    private String value;
    private boolean hidden;
    private boolean oldHidden;

    public SynonymEditRow() {
    }

    public SynonymEditRow(String lang, String value, boolean hidden) {
        this.lang = lang;
        this.oldValue = value;
        this.value = value;
        this.hidden = hidden;
        this.oldHidden = hidden;
    }
}
