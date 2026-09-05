package fr.cnrs.opentheso.v2.sync.service;

import fr.cnrs.opentheso.v2.concept.model.ConceptFullSnapshot;
import fr.cnrs.opentheso.v2.concept.model.ConceptSnapshotNote;
import fr.cnrs.opentheso.v2.concept.model.ConceptTermLabel;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldAction;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldCategory;
import fr.cnrs.opentheso.v2.proposition.model.PropositionFieldChange;
import fr.cnrs.opentheso.v2.sync.model.SyncConceptPayload;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Calcule le diff entre un concept reçu (esclave) et le concept maître courant.
 */
@Component
public class ThesaurusSyncConceptDiffService {

    public PropositionDraft diff(
            SyncConceptPayload incoming,
            ConceptFullSnapshot master,
            String workLang
    ) {
        PropositionDraft draft = new PropositionDraft();
        draft.setConceptId(master.getIdentifier());
        draft.setLang(workLang);
        applyPrefAndTranslations(draft, incoming, master, workLang);
        applyAltLabels(draft, incoming, master);

        compareNotes(draft, PropositionFieldCategory.NOTE, collectNotes(master.getNotes()), incoming.notes());
        compareNotes(draft, PropositionFieldCategory.DEFINITION, collectNotes(master.getDefinitions()), incoming.definitions());
        compareNotes(draft, PropositionFieldCategory.SCOPE, collectNotes(master.getScopeNotes()), incoming.scopeNotes());

        return draft;
    }

    private void applyPrefAndTranslations(
            PropositionDraft draft,
            SyncConceptPayload incoming,
            ConceptFullSnapshot master,
            String workLang
    ) {
        Map<String, String> masterPref = collectPrefLabels(master);
        Map<String, String> incomingPref = incoming.prefLabels() == null ? Map.of() : incoming.prefLabels();

        String masterPrefWork = masterPref.get(workLang);
        String incomingPrefWork = incomingPref.get(workLang);
        if (StringUtils.isNotBlank(incomingPrefWork)
                && !Objects.equals(normalize(masterPrefWork), normalize(incomingPrefWork))) {
            draft.setPreferredLabelChange(new PropositionFieldChange(
                    PropositionFieldCategory.NOM,
                    StringUtils.isBlank(masterPrefWork)
                            ? PropositionFieldAction.ADD
                            : PropositionFieldAction.UPDATE,
                    workLang,
                    incomingPrefWork,
                    masterPrefWork,
                    false
            ));
        }

        for (Map.Entry<String, String> entry : incomingPref.entrySet()) {
            String lang = entry.getKey();
            if (workLang.equalsIgnoreCase(lang) || StringUtils.isBlank(entry.getValue())) {
                continue;
            }
            String oldValue = masterPref.get(lang);
            if (StringUtils.isBlank(oldValue)) {
                draft.getTranslationChanges().add(new PropositionFieldChange(
                        PropositionFieldCategory.TRADUCTION,
                        PropositionFieldAction.ADD,
                        lang,
                        entry.getValue(),
                        null,
                        false
                ));
            } else if (!Objects.equals(normalize(oldValue), normalize(entry.getValue()))) {
                draft.getTranslationChanges().add(new PropositionFieldChange(
                        PropositionFieldCategory.TRADUCTION,
                        PropositionFieldAction.UPDATE,
                        lang,
                        entry.getValue(),
                        oldValue,
                        false
                ));
            }
        }
    }

    private void applyAltLabels(PropositionDraft draft, SyncConceptPayload incoming, ConceptFullSnapshot master) {
        Map<String, Set<String>> masterAlts = collectAltLabels(master);
        Map<String, List<String>> incomingAlts = incoming.altLabels() == null ? Map.of() : incoming.altLabels();
        for (Map.Entry<String, List<String>> entry : incomingAlts.entrySet()) {
            String lang = entry.getKey();
            Set<String> existing = masterAlts.getOrDefault(lang, Set.of());
            for (String value : nullSafe(entry.getValue())) {
                if (StringUtils.isBlank(value)) {
                    continue;
                }
                boolean known = existing.stream()
                        .anyMatch(current -> Objects.equals(normalize(current), normalize(value)));
                if (!known) {
                    draft.getSynonymChanges().add(new PropositionFieldChange(
                            PropositionFieldCategory.SYNONYME,
                            PropositionFieldAction.ADD,
                            lang,
                            value,
                            null,
                            false
                    ));
                }
            }
        }
    }

    private void compareNotes(
            PropositionDraft draft,
            PropositionFieldCategory category,
            Map<String, String> masterByLang,
            Map<String, List<String>> incomingByLang
    ) {
        if (incomingByLang == null || incomingByLang.isEmpty()) {
            return;
        }
        // Une seule note proposée par type (modèle proposition actuel).
        if (draft.getNoteChange(category.noteTypeCode()) != null) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : incomingByLang.entrySet()) {
            String lang = entry.getKey();
            String incomingValue = firstNonBlank(entry.getValue());
            if (StringUtils.isBlank(incomingValue)) {
                continue;
            }
            String masterValue = masterByLang.get(lang);
            if (StringUtils.isBlank(masterValue)) {
                draft.setNoteChange(new PropositionFieldChange(
                        category, PropositionFieldAction.ADD, lang, incomingValue, null, false));
                return;
            }
            if (!Objects.equals(normalize(masterValue), normalize(incomingValue))) {
                draft.setNoteChange(new PropositionFieldChange(
                        category, PropositionFieldAction.UPDATE, lang, incomingValue, masterValue, false));
                return;
            }
        }
    }

    private Map<String, String> collectPrefLabels(ConceptFullSnapshot master) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (master.getPrefLabel() != null && StringUtils.isNotBlank(master.getPrefLabel().value())) {
            labels.put(master.getPrefLabel().lang(), master.getPrefLabel().value());
        }
        if (CollectionUtils.isNotEmpty(master.getPrefLabelsTraduction())) {
            for (ConceptTermLabel label : master.getPrefLabelsTraduction()) {
                if (StringUtils.isNotBlank(label.value())) {
                    labels.putIfAbsent(label.lang(), label.value());
                }
            }
        }
        return labels;
    }

    private Map<String, Set<String>> collectAltLabels(ConceptFullSnapshot master) {
        Map<String, Set<String>> labels = new LinkedHashMap<>();
        addAlt(labels, master.getAltLabels());
        addAlt(labels, master.getAltLabelTraduction());
        return labels;
    }

    private void addAlt(Map<String, Set<String>> target, List<ConceptTermLabel> source) {
        if (CollectionUtils.isEmpty(source)) {
            return;
        }
        for (ConceptTermLabel label : source) {
            if (StringUtils.isBlank(label.value())) {
                continue;
            }
            target.computeIfAbsent(label.lang(), ignored -> new LinkedHashSet<>()).add(label.value());
        }
    }

    private Map<String, String> collectNotes(List<ConceptSnapshotNote> notes) {
        Map<String, String> byLang = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(notes)) {
            return byLang;
        }
        for (ConceptSnapshotNote note : notes) {
            if (StringUtils.isNotBlank(note.value())) {
                byLang.putIfAbsent(note.lang(), note.value());
            }
        }
        return byLang;
    }

    private static String firstNonBlank(Collection<String> values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static List<String> nullSafe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
