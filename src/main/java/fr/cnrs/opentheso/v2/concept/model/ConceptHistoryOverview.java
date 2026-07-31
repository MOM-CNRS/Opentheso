package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;

public record ConceptHistoryOverview(
        List<ConceptHistoryEntry> labels,
        List<ConceptHistoryEntry> synonyms,
        List<ConceptHistoryEntry> relations,
        List<ConceptHistoryEntry> notes
) {

    public List<ConceptHistoryEntry> getLabels() {
        return labels;
    }

    public List<ConceptHistoryEntry> getSynonyms() {
        return synonyms;
    }

    public List<ConceptHistoryEntry> getRelations() {
        return relations;
    }

    public List<ConceptHistoryEntry> getNotes() {
        return notes;
    }

    public boolean isEmpty() {
        return labels.isEmpty() && synonyms.isEmpty() && relations.isEmpty() && notes.isEmpty();
    }
}
