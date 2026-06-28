package fr.cnrs.opentheso.v2.shared.session;

import java.io.Serializable;

public record ThesaurusSelection(String thesaurusId, String title) implements Serializable {
}
