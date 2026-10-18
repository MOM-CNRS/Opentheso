package fr.cnrs.opentheso.shared.session;

import java.io.Serializable;

public record ThesaurusSelection(String thesaurusId, String title) implements Serializable {
}
