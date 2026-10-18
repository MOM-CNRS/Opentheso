package fr.cnrs.opentheso.setting.service;

import fr.cnrs.opentheso.concept.alignment.model.AlignmentSourceItem;

import java.util.List;
import java.util.Set;

public record AlignmentPersistDraft(
        List<AlignmentSourceItem> current,
        List<AlignmentSourceItem> baseline,
        Set<Integer> idsToDelete
) {
}
