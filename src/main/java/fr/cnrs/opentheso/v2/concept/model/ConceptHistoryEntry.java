package fr.cnrs.opentheso.v2.concept.model;

import java.util.Date;

public record ConceptHistoryEntry(
        String value,
        String lang,
        String action,
        Date date,
        String user,
        String noteType,
        String role
) {

    public String getValue() {
        return value;
    }

    public String getLang() {
        return lang;
    }

    public String getAction() {
        return action;
    }

    public Date getDate() {
        return date;
    }

    public String getUser() {
        return user;
    }

    public String getNoteType() {
        return noteType;
    }

    public String getRole() {
        return role;
    }
}
