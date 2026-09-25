package fr.cnrs.opentheso.v2.sync.model;

import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;

import java.util.ArrayList;
import java.util.List;

public final class SyncFieldChanges {

    private SyncFieldChanges() {
    }

    public static List<SyncFieldChange> fromDraft(PropositionDraft draft) {
        if (draft == null) {
            return List.of();
        }
        List<SyncFieldChange> changes = new ArrayList<>();
        add(changes, draft.getPreferredLabelChange());
        if (draft.getTranslationChanges() != null) {
            draft.getTranslationChanges().forEach(change -> add(changes, change));
        }
        if (draft.getSynonymChanges() != null) {
            draft.getSynonymChanges().forEach(change -> add(changes, change));
        }
        if (draft.getNoteChanges() != null) {
            draft.getNoteChanges().values().forEach(change -> add(changes, change));
        }
        return List.copyOf(changes);
    }

    private static void add(List<SyncFieldChange> target, PropositionFieldChange change) {
        if (change == null || change.category() == null) {
            return;
        }
        target.add(new SyncFieldChange(
                fieldKey(change.category()),
                change.lang(),
                change.action() == null ? "" : change.action().name(),
                change.oldValue(),
                change.value()
        ));
    }

    public static String fieldKey(PropositionFieldCategory category) {
        if (category == null) {
            return "concept";
        }
        return switch (category) {
            case NOM -> "prefLabel";
            case TRADUCTION -> "translation";
            case SYNONYME -> "synonym";
            case DEFINITION -> "definition";
            case SCOPE -> "scopeNote";
            default -> "note";
        };
    }
}
