package fr.cnrs.opentheso.models;

import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;

public interface PropositionProjection {

    Integer getId();
    String getIdConcept();
    String getLang();
    String getIdTheso();
    String getStatus();

    String getDate();

    String getNom();
    String getEmail();
    String getCommentaire();
    String getApprouvePar();

    Instant getApprouveDate();
    String getAdminComment();
    String getLexicalValue();
    String getCodePays();
}

