package fr.cnrs.opentheso.v2.proposition.model;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public final class PropositionDraftMapper {

    private PropositionDraftMapper() {
    }

    public static PropositionDraft toDraft(
            String conceptId,
            String thesaurusId,
            String lang,
            String currentPreferredLabel,
            String proposedPreferredLabel,
            List<PropositionSynonymOption> synonymOptions,
            List<PropositionTranslationOption> translationOptions,
            List<PropositionNoteOption> noteOptions
    ) {
        var draft = new PropositionDraft();
        draft.setConceptId(conceptId);
        draft.setThesaurusId(thesaurusId);
        draft.setLang(lang);

        if (StringUtils.isNotBlank(proposedPreferredLabel)
                && !proposedPreferredLabel.trim().equals(StringUtils.defaultString(currentPreferredLabel).trim())) {
            draft.setPreferredLabelChange(new PropositionFieldChange(
                    PropositionFieldCategory.NOM,
                    PropositionFieldAction.UPDATE,
                    lang,
                    proposedPreferredLabel.trim(),
                    currentPreferredLabel,
                    false
            ));
        }

        if (synonymOptions != null) {
            synonymOptions.stream()
                    .filter(option -> option.isToAdd() || option.isToUpdate() || option.isToRemove())
                    .map(PropositionDraftMapper::toSynonymChange)
                    .forEach(draft.getSynonymChanges()::add);
        }

        if (translationOptions != null) {
            translationOptions.stream()
                    .filter(option -> option.isToAdd() || option.isToUpdate() || option.isToRemove())
                    .map(PropositionDraftMapper::toTranslationChange)
                    .forEach(draft.getTranslationChanges()::add);
        }

        if (noteOptions != null) {
            for (var option : noteOptions) {
                if (!option.hasChanged()) {
                    continue;
                }
                var action = resolveNoteAction(option);
                var category = PropositionFieldCategory.forNoteType(option.getTypeCode());
                draft.setNoteChange(new PropositionFieldChange(
                        category, action, lang, option.getValue(), option.getOldValue(), false));
            }
        }

        return draft;
    }

    private static PropositionFieldChange toSynonymChange(PropositionSynonymOption option) {
        return new PropositionFieldChange(
                PropositionFieldCategory.SYNONYME,
                resolveAction(option.isToAdd(), option.isToRemove()),
                option.getLang(),
                option.getValue(),
                option.getOldValue(),
                option.isHidden()
        );
    }

    private static PropositionFieldChange toTranslationChange(PropositionTranslationOption option) {
        return new PropositionFieldChange(
                PropositionFieldCategory.TRADUCTION,
                resolveAction(option.isToAdd(), option.isToRemove()),
                option.getLang(),
                option.getValue(),
                option.getOldValue(),
                false
        );
    }

    private static PropositionFieldAction resolveAction(boolean toAdd, boolean toRemove) {
        if (toAdd) {
            return PropositionFieldAction.ADD;
        }
        if (toRemove) {
            return PropositionFieldAction.DELETE;
        }
        return PropositionFieldAction.UPDATE;
    }

    private static PropositionFieldAction resolveNoteAction(PropositionNoteOption option) {
        boolean hadValue = StringUtils.isNotBlank(option.getOldValue());
        boolean hasValue = StringUtils.isNotBlank(option.getValue());
        if (!hadValue && hasValue) {
            return PropositionFieldAction.ADD;
        }
        if (hadValue && !hasValue) {
            return PropositionFieldAction.DELETE;
        }
        return PropositionFieldAction.UPDATE;
    }
}
