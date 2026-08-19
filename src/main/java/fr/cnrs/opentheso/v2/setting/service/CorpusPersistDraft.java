package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;

import java.util.List;
import java.util.Map;

public record CorpusPersistDraft(
        List<ThesaurusCorpus> current,
        List<ThesaurusCorpus> baseline,
        Map<String, String> originalNameByCurrent
) {
}
