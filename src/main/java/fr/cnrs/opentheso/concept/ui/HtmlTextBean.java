package fr.cnrs.opentheso.concept.ui;

import fr.cnrs.opentheso.utils.HtmlText;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("htmlText")
@ApplicationScoped
public class HtmlTextBean implements Serializable {

    public String format(String value) {
        return HtmlText.format(value);
    }

    public String formatInline(String value) {
        return HtmlText.formatInline(value);
    }
}
