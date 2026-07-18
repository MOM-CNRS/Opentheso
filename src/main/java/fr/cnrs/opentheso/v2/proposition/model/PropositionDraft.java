package fr.cnrs.opentheso.v2.proposition.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class PropositionDraft implements Serializable {

    private String conceptId;
    private String thesaurusId;
    private String lang;

    private PropositionFieldChange preferredLabelChange;
    private List<PropositionFieldChange> synonymChanges = new ArrayList<>();
    private List<PropositionFieldChange> translationChanges = new ArrayList<>();
    private Map<String, PropositionFieldChange> noteChanges = new LinkedHashMap<>();

    public void setNoteChange(PropositionFieldChange change) {
        noteChanges.put(change.category().noteTypeCode(), change);
    }

    public PropositionFieldChange getNoteChange(String noteTypeCode) {
        return noteChanges.get(noteTypeCode);
    }

    public boolean isEmpty() {
        return preferredLabelChange == null
                && synonymChanges.isEmpty()
                && translationChanges.isEmpty()
                && noteChanges.isEmpty();
    }
}
