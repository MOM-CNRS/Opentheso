package fr.cnrs.opentheso.v2.concept.write.ui;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class TranslationBlockEditRow implements Serializable {

    private String lang;
    private String value;
    private String alts;
    private boolean existing;

    public TranslationBlockEditRow(String lang, String value, String alts, boolean existing) {
        this.lang = lang;
        this.value = value;
        this.alts = alts;
        this.existing = existing;
    }
}
