package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptLabel;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Édition inline du bloc Traductions (autres langues que la langue de travail).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptTranslationBlockEditorBean")
@RequiredArgsConstructor
public class ConceptTranslationBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "traductions";

    private final ThesaurusViewBean thesaurusViewBean;
    private final ConceptLexicalMutationService conceptLexicalMutationService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final ConceptWritePolicy conceptWritePolicy;
    private final UserSession userSession;
    private final ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private String editingLang;
    private List<TranslationBlockEditRow> rows = new ArrayList<>();
    private List<ConceptWriteLanguage> thesaurusLanguages = new ArrayList<>();
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedConcept() != null
                && conceptWritePolicy.canMutateLexicalContent(
                        userSession, thesaurusViewBean.isSelectedConceptDeprecated());
    }

    public boolean isEditing() {
        if (editing && !matchesCurrentConcept()) {
            resetForm(false);
        }
        return editing && FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard());
    }

    public boolean isCanAddRow() {
        return isEditing() && !languagesForNewRow().isEmpty();
    }

    public void startEditing() {
        if (!isEditable()) {
            return;
        }
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return;
        }
        editingConceptId = detail.getSummary().getConceptId();
        editingLang = resolveWorkLang(detail);
        thesaurusLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusViewBean.getId(), editingLang);
        rows = copyRows(detail, editingLang);
        errorMessage = "";
        flashMessage = "";
        flashToken = "";
        editing = true;
        thesaurusViewBean.setFicheEditCard(FICHE_CARD);
        conceptSelectionContext.update(thesaurusViewBean.getId(), detail);
    }

    public void cancel() {
        resetForm(false);
    }

    public void addRow() {
        if (!isEditing()) {
            return;
        }
        List<ConceptWriteLanguage> available = languagesForNewRow();
        if (available.isEmpty()) {
            return;
        }
        rows.add(new TranslationBlockEditRow(available.get(0).code(), "", "", false));
    }

    public void removeRow(int index) {
        if (!isEditing() || index < 0 || index >= rows.size()) {
            return;
        }
        rows.remove(index);
    }

    public List<ConceptWriteLanguage> languagesFor(TranslationBlockEditRow row) {
        Set<String> taken = usedLangsExcluding(row);
        String work = normalizeLang(editingLang);
        String current = row == null ? "" : normalizeLang(row.getLang());
        return thesaurusLanguages.stream()
                .filter(lang -> lang != null && StringUtils.isNotBlank(lang.code()))
                .filter(lang -> {
                    String code = normalizeLang(lang.code());
                    if (code.equals(work)) {
                        return false;
                    }
                    return code.equals(current) || !taken.contains(code);
                })
                .toList();
    }

    public void save() {
        errorMessage = "";
        if (!isEditable() || !isEditing()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            errorMessage = "Action non autorisée";
            return;
        }
        ConceptDetail current = thesaurusViewBean.getSelectedConcept();
        if (current == null || current.getSummary() == null) {
            errorMessage = "Action non autorisée";
            return;
        }

        String workLang = resolveWorkLang(current);
        Map<String, TranslationBlockEditRow> selected = new LinkedHashMap<>();
        for (TranslationBlockEditRow row : rows) {
            if (row == null) {
                continue;
            }
            String lang = normalizeLang(row.getLang());
            String value = StringUtils.trimToEmpty(row.getValue());
            if (lang.isEmpty()) {
                errorMessage = "Aucune langue sélectionnée !";
                return;
            }
            if (lang.equals(normalizeLang(workLang))) {
                errorMessage = "La langue de travail s'édite dans le bloc Libellé.";
                return;
            }
            if (value.isEmpty()) {
                errorMessage = "La valeur est obligatoire !";
                return;
            }
            if (selected.put(lang, row) != null) {
                errorMessage = "Chaque langue ne peut apparaître qu'une fois.";
                return;
            }
            List<String> alts = ConceptLabelBlockEditorBean.parseCsv(row.getAlts());
            if (alts.stream().anyMatch(alt -> alt.equalsIgnoreCase(value))) {
                errorMessage = "Une forme alternative ne peut pas être identique au libellé.";
                return;
            }
        }

        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        Map<String, String> oldPrefs = preferredByLang(current, workLang);
        Map<String, List<String>> oldAlts = altsByLang(current, workLang);
        boolean dirty = false;

        LinkedHashSet<String> removedLangs = new LinkedHashSet<>();
        removedLangs.addAll(oldPrefs.keySet());
        removedLangs.addAll(oldAlts.keySet());
        removedLangs.removeAll(selected.keySet());
        for (String lang : removedLangs) {
            if (!syncAlts(thesaurusId, conceptId, lang, userId, contributor,
                    oldAlts.getOrDefault(lang, List.of()), List.of(), dirty)) {
                return;
            }
            dirty = dirty || !oldAlts.getOrDefault(lang, List.of()).isEmpty();
            if (!oldPrefs.containsKey(lang)) {
                continue;
            }
            MutationResult deleted = conceptLexicalMutationService.deleteTranslation(
                    new DeleteTranslationCommand(thesaurusId, conceptId, lang, userId, contributor));
            if (!applyResult(deleted, dirty)) {
                return;
            }
            dirty = true;
        }

        for (TranslationBlockEditRow row : selected.values()) {
            String lang = normalizeLang(row.getLang());
            String value = StringUtils.trimToEmpty(row.getValue());
            List<String> newAlts = ConceptLabelBlockEditorBean.parseCsv(row.getAlts());
            List<String> previousAlts = oldAlts.getOrDefault(lang, List.of());
            if (!oldPrefs.containsKey(lang)) {
                MutationResult added = conceptLexicalMutationService.addTranslation(
                        new AddTranslationCommand(thesaurusId, conceptId, lang, value, userId, contributor));
                if (!applyResult(added, dirty)) {
                    return;
                }
                dirty = true;
                if (!syncAlts(thesaurusId, conceptId, lang, userId, contributor, previousAlts, newAlts, dirty)) {
                    return;
                }
                continue;
            }
            if (!StringUtils.equals(oldPrefs.get(lang), value)) {
                MutationResult updated = conceptLexicalMutationService.updateTranslation(
                        new UpdateTranslationCommand(thesaurusId, conceptId, lang, value, userId, contributor));
                if (!applyResult(updated, dirty)) {
                    return;
                }
                dirty = true;
            }
            if (!syncAlts(thesaurusId, conceptId, lang, userId, contributor, previousAlts, newAlts, dirty)) {
                return;
            }
            dirty = dirty || !previousAlts.equals(newAlts);
        }

        finishSuccess();
    }

    private boolean syncAlts(
            String thesaurusId,
            String conceptId,
            String lang,
            int userId,
            String contributor,
            List<String> oldAlts,
            List<String> newAlts,
            boolean dirty
    ) {
        Set<String> oldSet = new LinkedHashSet<>(oldAlts);
        Set<String> newSet = new LinkedHashSet<>(newAlts);
        for (String value : oldAlts) {
            if (newSet.contains(value)) {
                continue;
            }
            MutationResult deleted = conceptLexicalMutationService.deleteSynonym(
                    new DeleteSynonymCommand(thesaurusId, conceptId, lang, value, userId, contributor));
            if (!applyResult(deleted, dirty)) {
                return false;
            }
            dirty = true;
        }
        for (String value : newAlts) {
            if (oldSet.contains(value)) {
                continue;
            }
            MutationResult added = conceptLexicalMutationService.addSynonym(
                    new AddSynonymCommand(thesaurusId, conceptId, lang, value, false, userId, contributor, false));
            if (!applyResult(added, dirty)) {
                return false;
            }
            dirty = true;
        }
        return true;
    }

    private boolean applyResult(MutationResult result, boolean dirty) {
        if (result == null) {
            errorMessage = "L'enregistrement a échoué.";
            reloadIfDirty(dirty);
            return false;
        }
        if (result.outcome() == MutationOutcome.OK) {
            return true;
        }
        errorMessage = StringUtils.defaultIfBlank(result.message(), "L'enregistrement a échoué.");
        reloadIfDirty(dirty);
        return false;
    }

    private void finishSuccess() {
        editing = false;
        if (FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        errorMessage = "";
        flashMessage = "Traductions enregistrées";
        flashToken = String.valueOf(System.currentTimeMillis());
        thesaurusViewBean.reloadSelectedConcept();
        conceptSelectionContext.update(thesaurusViewBean.getId(), thesaurusViewBean.getSelectedConcept());
    }

    private void reloadIfDirty(boolean dirty) {
        if (dirty) {
            thesaurusViewBean.reloadSelectedConcept();
        }
    }

    private void resetForm(boolean keepFlash) {
        editing = false;
        if (FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        editingConceptId = null;
        editingLang = null;
        rows = new ArrayList<>();
        thesaurusLanguages = new ArrayList<>();
        errorMessage = "";
        if (!keepFlash) {
            flashMessage = "";
            flashToken = "";
        }
    }

    private boolean matchesCurrentConcept() {
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return false;
        }
        return StringUtils.equals(editingConceptId, detail.getSummary().getConceptId());
    }

    private List<ConceptWriteLanguage> languagesForNewRow() {
        Set<String> taken = usedLangsExcluding(null);
        taken.add(normalizeLang(editingLang));
        return thesaurusLanguages.stream()
                .filter(lang -> lang != null && StringUtils.isNotBlank(lang.code()))
                .filter(lang -> !taken.contains(normalizeLang(lang.code())))
                .toList();
    }

    private Set<String> usedLangsExcluding(TranslationBlockEditRow skip) {
        return rows.stream()
                .filter(row -> row != skip)
                .map(TranslationBlockEditRow::getLang)
                .map(ConceptTranslationBlockEditorBean::normalizeLang)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveWorkLang(ConceptDetail detail) {
        if (detail != null && detail.getSummary() != null
                && StringUtils.isNotBlank(detail.getSummary().getLang())) {
            return detail.getSummary().getLang();
        }
        return StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
    }

    private static List<TranslationBlockEditRow> copyRows(ConceptDetail detail, String workLang) {
        List<TranslationBlockEditRow> copied = new ArrayList<>();
        Map<String, String> prefs = preferredByLang(detail, workLang);
        Map<String, List<String>> alts = altsByLang(detail, workLang);
        LinkedHashSet<String> langs = new LinkedHashSet<>();
        langs.addAll(prefs.keySet());
        langs.addAll(alts.keySet());
        for (String lang : langs) {
            copied.add(new TranslationBlockEditRow(
                    lang,
                    prefs.getOrDefault(lang, ""),
                    ConceptLabelBlockEditorBean.joinCsv(alts.getOrDefault(lang, List.of())),
                    prefs.containsKey(lang)));
        }
        return copied;
    }

    private static Map<String, String> preferredByLang(ConceptDetail detail, String workLang) {
        Map<String, String> byLang = new LinkedHashMap<>();
        String work = normalizeLang(workLang);
        if (detail == null || detail.getTranslations() == null) {
            return byLang;
        }
        for (ConceptLabel label : detail.getTranslations()) {
            if (label == null || !label.isPreferred() || StringUtils.isBlank(label.getLang())) {
                continue;
            }
            String lang = normalizeLang(label.getLang());
            if (lang.equals(work)) {
                continue;
            }
            byLang.putIfAbsent(lang, StringUtils.defaultString(label.getValue()));
        }
        return byLang;
    }

    private static Map<String, List<String>> altsByLang(ConceptDetail detail, String workLang) {
        Map<String, List<String>> byLang = new LinkedHashMap<>();
        String work = normalizeLang(workLang);
        if (detail == null || detail.getTranslations() == null) {
            return byLang;
        }
        for (ConceptLabel label : detail.getTranslations()) {
            if (label == null || label.isPreferred() || label.isHidden() || StringUtils.isBlank(label.getLang())) {
                continue;
            }
            String lang = normalizeLang(label.getLang());
            if (lang.equals(work)) {
                continue;
            }
            String value = StringUtils.trimToEmpty(label.getValue());
            if (value.isEmpty()) {
                continue;
            }
            byLang.computeIfAbsent(lang, key -> new ArrayList<>()).add(value);
        }
        return byLang;
    }

    private static String normalizeLang(String lang) {
        return StringUtils.isBlank(lang) ? "" : lang.trim().toLowerCase(Locale.ROOT);
    }
}
