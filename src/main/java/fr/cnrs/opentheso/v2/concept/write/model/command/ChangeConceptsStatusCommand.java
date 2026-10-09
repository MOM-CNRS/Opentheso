package fr.cnrs.opentheso.v2.concept.write.model.command;

import java.util.List;

/**
 * Changement de statut groupé. {@code action} : {@code approve} (candidats → validés)
 * ou {@code deprecate} (concepts actifs → obsolètes).
 */
public record ChangeConceptsStatusCommand(
        String thesaurusId,
        List<String> conceptIds,
        String action,
        int userId,
        String contributorName
) {
    public ChangeConceptsStatusCommand {
        conceptIds = conceptIds == null ? List.of() : List.copyOf(conceptIds);
        action = action == null ? "" : action.trim().toLowerCase();
    }

    public boolean approve() {
        return "approve".equals(action);
    }

    public boolean deprecate() {
        return "deprecate".equals(action);
    }
}
