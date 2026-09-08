package fr.cnrs.opentheso.v2.concept.model;

import java.io.Serializable;
import java.time.Instant;

public record ConceptHistoryEntry(
        String value,
        String lang,
        String action,
        Instant date,
        String user,
        String noteType,
        String role
) implements Serializable {

    public String getValue() {
        return value;
    }

    public String getLang() {
        return lang;
    }

    public String getAction() {
        return action;
    }

    public Instant getDate() {
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
