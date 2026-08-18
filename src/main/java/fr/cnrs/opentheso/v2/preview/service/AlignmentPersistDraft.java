package fr.cnrs.opentheso.v2.preview.service;

import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;

import java.util.List;
import java.util.Set;

public record AlignmentPersistDraft(
        List<AlignmentSourceItem> current,
        List<AlignmentSourceItem> baseline,
        Set<Integer> idsToDelete
) {
}
