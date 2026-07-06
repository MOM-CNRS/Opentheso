package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TranslationEditRow implements Serializable {

    private String lang;
    private String value;

    public TranslationEditRow() {
    }

    public TranslationEditRow(String lang, String value) {
        this.lang = lang;
        this.value = value;
    }
}
