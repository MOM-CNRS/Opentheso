package fr.cnrs.opentheso.candidat.model;

import java.util.Set;

/**
 * Types de notes acceptés à la création d'un candidat (table {@code note_type}).
 */
public final class CandidateNoteTypes {

    public static final Set<String> CODES = Set.of(
            "definition",
            "note",
            "scopeNote",
            "historyNote",
            "editorialNote",
            "changeNote",
            "example"
    );

    private CandidateNoteTypes() {
    }

    public static boolean isSupported(String code) {
        return code != null && CODES.contains(code);
    }
}
