package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.v2.concept.alignment.model.AlignmentSourceItem;
import fr.cnrs.opentheso.v2.concept.alignment.service.ConceptAlignmentAdminService;
import fr.cnrs.opentheso.v2.setting.exception.InvalidSettingDataException;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusCorpus;
import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.ui.IdentifierServerSelection;
import fr.cnrs.opentheso.v2.setting.ui.PreferenceEditor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enregistre préférences, corpus et sources d'alignement dans une seule transaction.
 */
@Service
@RequiredArgsConstructor
public class ThesaurusSettingsPersistService {

    private final ThesaurusPreferenceService thesaurusPreferenceService;
    private final ThesaurusCorpusService thesaurusCorpusService;
    private final ConceptAlignmentAdminService conceptAlignmentAdminService;
    private final ThesaurusSearchLanguageSync thesaurusSearchLanguageSync;

    @Transactional(rollbackFor = Exception.class)
    public ThesaurusPreferences saveAll(
            String thesaurusId,
            Integer userId,
            PreferenceEditor editor,
            String workLanguage,
            CorpusPersistDraft corpusDraft,
            AlignmentPersistDraft alignmentDraft
    ) {
        if (editor == null) {
            throw new InvalidSettingDataException("Aucune préférence à enregistrer.");
        }
        IdentifierServerSelection.syncType(editor);
        ThesaurusPreferences saved = thesaurusPreferenceService.savePreferences(
                thesaurusId,
                editor.toModel(thesaurusId),
                editor.getNewPassArk(),
                editor.getNewPassHandle(),
                editor.getNewDeeplApiKey(),
                editor.getNewApiKeyOpenArk(),
                workLanguage
        );
        persistCorpus(thesaurusId, corpusDraft);
        persistAlignment(thesaurusId, userId, alignmentDraft);
        if (StringUtils.isNotBlank(saved.sourceLang())) {
            thesaurusSearchLanguageSync.applyAfterSourceLanguageChange(thesaurusId, saved.sourceLang());
        }
        return saved;
    }

    private void persistCorpus(String thesaurusId, CorpusPersistDraft draft) {
        if (draft == null) {
            return;
        }
        List<ThesaurusCorpus> current = draft.current() != null ? draft.current() : List.of();
        List<ThesaurusCorpus> baseline = draft.baseline() != null ? draft.baseline() : List.of();
        Map<String, String> originalByCurrent = draft.originalNameByCurrent() != null
                ? draft.originalNameByCurrent()
                : Map.of();
        Set<String> keptOriginals = new HashSet<>(originalByCurrent.values());
        for (ThesaurusCorpus original : baseline) {
            if (!keptOriginals.contains(original.corpusName())) {
                thesaurusCorpusService.deleteCorpus(thesaurusId, original.corpusName());
            }
        }
        for (ThesaurusCorpus item : current) {
            String originalName = originalByCurrent.get(item.corpusName());
            if (originalName == null) {
                thesaurusCorpusService.createCorpus(thesaurusId, item);
            } else if (!sameCorpus(findBaselineCorpus(baseline, originalName), item)
                    || !originalName.equals(item.corpusName())) {
                thesaurusCorpusService.updateCorpus(thesaurusId, originalName, item);
            }
        }
    }

    private void persistAlignment(String thesaurusId, Integer userId, AlignmentPersistDraft draft) {
        if (draft == null) {
            return;
        }
        if (userId == null) {
            throw new InvalidSettingDataException(WriteUiMessages.UNAUTHORIZED_FALLBACK);
        }
        Set<Integer> toDelete = draft.idsToDelete() != null ? draft.idsToDelete() : Set.of();
        for (Integer sourceId : new ArrayList<>(toDelete)) {
            conceptAlignmentAdminService.deleteLocalSource(sourceId);
        }
        List<AlignmentSourceItem> current = draft.current() != null ? draft.current() : List.of();
        List<AlignmentSourceItem> baseline = draft.baseline() != null ? draft.baseline() : List.of();
        for (AlignmentSourceItem item : current) {
            persistAlignmentItem(thesaurusId, userId, item, baseline);
        }
    }

    private void persistAlignmentItem(
            String thesaurusId,
            Integer userId,
            AlignmentSourceItem item,
            List<AlignmentSourceItem> baseline
    ) {
        if (item.getSourceId() < 0) {
            String error = conceptAlignmentAdminService.addLocalSource(
                    thesaurusId,
                    userId,
                    item.getLabel(),
                    item.getUrl(),
                    item.getDescription(),
                    item.getSourceType(),
                    item.isSelected()
            );
            if (error != null) {
                throw new InvalidSettingDataException(error);
            }
            return;
        }
        AlignmentSourceItem previous = findBaselineAlignment(baseline, item.getSourceId());
        if (previous != null && alignmentMetadataChanged(previous, item)) {
            String error = conceptAlignmentAdminService.updateLocalSource(
                    item.getSourceId(),
                    item.getLabel(),
                    item.getUrl(),
                    item.getDescription(),
                    item.getSourceType());
            if (error != null) {
                throw new InvalidSettingDataException(error);
            }
        }
        if (previous == null || previous.isSelected() != item.isSelected()) {
            conceptAlignmentAdminService.setSourceSelected(thesaurusId, item.getSourceId(), item.isSelected());
        }
    }

    private static ThesaurusCorpus findBaselineCorpus(List<ThesaurusCorpus> baseline, String originalName) {
        return baseline.stream()
                .filter(item -> originalName.equals(item.corpusName()))
                .findFirst()
                .orElse(null);
    }

    private static boolean sameCorpus(ThesaurusCorpus left, ThesaurusCorpus right) {
        if (left == null || right == null) {
            return false;
        }
        return StringUtils.equals(left.corpusName(), right.corpusName())
                && StringUtils.equals(left.uriLink(), right.uriLink())
                && StringUtils.equals(left.uriCount(), right.uriCount())
                && left.active() == right.active()
                && left.onlyUriLink() == right.onlyUriLink()
                && left.omekaS() == right.omekaS();
    }

    private static AlignmentSourceItem findBaselineAlignment(List<AlignmentSourceItem> baseline, int sourceId) {
        return baseline.stream()
                .filter(item -> item.getSourceId() == sourceId)
                .findFirst()
                .orElse(null);
    }

    private static boolean alignmentMetadataChanged(AlignmentSourceItem baseline, AlignmentSourceItem current) {
        return !StringUtils.equals(baseline.getLabel(), current.getLabel())
                || !StringUtils.equals(baseline.getUrl(), current.getUrl())
                || !StringUtils.equals(baseline.getDescription(), current.getDescription())
                || !StringUtils.equals(baseline.getSourceType(), current.getSourceType());
    }
}
