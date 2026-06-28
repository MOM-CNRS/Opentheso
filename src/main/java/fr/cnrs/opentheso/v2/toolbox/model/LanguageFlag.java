package fr.cnrs.opentheso.v2.toolbox.model;

import java.io.Serializable;

public class LanguageFlag implements Serializable {

    private String iso6391;
    private String countryCode;

    public LanguageFlag() {
    }

    public LanguageFlag(String iso6391, String countryCode) {
        this.iso6391 = iso6391;
        this.countryCode = countryCode;
    }

    public String getIso6391() {
        return iso6391;
    }

    public void setIso6391(String iso6391) {
        this.iso6391 = iso6391;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
