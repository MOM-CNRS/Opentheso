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
import org.apache.commons.lang3.Strings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptLexicalMutationService conceptLexicalMutationService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

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
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        ConceptDetail current = thesaurusViewBean.getSelectedConcept();
        if (current == null || current.getSummary() == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }

        String workLang = resolveWorkLang(current);
        Optional<Map<String, TranslationBlockEditRow>> selected = collectSelectedTranslations(workLang);
        if (selected.isEmpty()) {
            return;
        }
        persistTranslationChanges(current, workLang, selected.get(), userId);
    }

    private Optional<Map<String, TranslationBlockEditRow>> collectSelectedTranslations(String workLang) {
        Map<String, TranslationBlockEditRow> selected = new LinkedHashMap<>();
        for (TranslationBlockEditRow row : rows) {
            if (!acceptSelectedTranslation(row, workLang, selected)) {
                return Optional.empty();
            }
        }
        return Optional.of(selected);
    }

    private boolean acceptSelectedTranslation(
            TranslationBlockEditRow row, String workLang, Map<String, TranslationBlockEditRow> selected) {
        if (row == null) {
            return true;
        }
        String lang = normalizeLang(row.getLang());
        String value = StringUtils.trimToEmpty(row.getValue());
        if (lang.isEmpty()) {
            errorMessage = "Aucune langue sélectionnée !";
            return false;
        }
        if (lang.equals(normalizeLang(workLang))) {
            errorMessage = "La langue de travail s'édite dans le bloc Libellé.";
            return false;
        }
        if (value.isEmpty()) {
            errorMessage = "La valeur est obligatoire !";
            return false;
        }
        if (selected.put(lang, row) != null) {
            errorMessage = "Chaque langue ne peut apparaître qu'une fois.";
            return false;
        }
        List<String> alts = ConceptLabelBlockEditorBean.parseCsv(row.getAlts());
        if (alts.stream().anyMatch(alt -> alt.equalsIgnoreCase(value))) {
            errorMessage = "Une forme alternative ne peut pas être identique au libellé.";
            return false;
        }
        return true;
    }

    private void persistTranslationChanges(
            ConceptDetail current,
            String workLang,
            Map<String, TranslationBlockEditRow> selected,
            int userId
    ) {
        TranslationWriteContext ctx = new TranslationWriteContext(
                thesaurusViewBean.getId(),
                current.getSummary().getConceptId(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()));
        Map<String, String> oldPrefs = preferredByLang(current, workLang);
        Map<String, List<String>> oldAlts = altsByLang(current, workLang);
        DirtyUpdate dirty = removeUnselectedLangs(ctx, selected, oldPrefs, oldAlts, false);
        if (!dirty.ok) {
            return;
        }
        if (!persistSelectedTranslations(ctx, selected, oldPrefs, oldAlts, dirty.dirty).ok) {
            return;
        }
        finishSuccess();
    }

    private DirtyUpdate removeUnselectedLangs(
            TranslationWriteContext ctx,
            Map<String, TranslationBlockEditRow> selected,
            Map<String, String> oldPrefs,
            Map<String, List<String>> oldAlts,
            boolean dirty
    ) {
        LinkedHashSet<String> removedLangs = new LinkedHashSet<>();
        removedLangs.addAll(oldPrefs.keySet());
        removedLangs.addAll(oldAlts.keySet());
        removedLangs.removeAll(selected.keySet());
        for (String lang : removedLangs) {
            DirtyUpdate next = removeUnselectedLang(ctx, lang, oldPrefs, oldAlts, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate removeUnselectedLang(
            TranslationWriteContext ctx,
            String lang,
            Map<String, String> oldPrefs,
            Map<String, List<String>> oldAlts,
            boolean dirty
    ) {
        List<String> previousAlts = oldAlts.getOrDefault(lang, List.of());
        if (!syncAlts(new TranslationAltSync(ctx, lang, previousAlts, List.of(), dirty))) {
            return DirtyUpdate.fail();
        }
        dirty = dirty || !previousAlts.isEmpty();
        if (!oldPrefs.containsKey(lang)) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult deleted = conceptLexicalMutationService.deleteTranslation(
                new DeleteTranslationCommand(
                        ctx.thesaurusId(), ctx.conceptId(), lang, ctx.userId(), ctx.contributor()));
        if (!applyResult(deleted, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate persistSelectedTranslations(
            TranslationWriteContext ctx,
            Map<String, TranslationBlockEditRow> selected,
            Map<String, String> oldPrefs,
            Map<String, List<String>> oldAlts,
            boolean dirty
    ) {
        for (TranslationBlockEditRow row : selected.values()) {
            DirtyUpdate next = persistSelectedTranslation(ctx, row, oldPrefs, oldAlts, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate persistSelectedTranslation(
            TranslationWriteContext ctx,
            TranslationBlockEditRow row,
            Map<String, String> oldPrefs,
            Map<String, List<String>> oldAlts,
            boolean dirty
    ) {
        String lang = normalizeLang(row.getLang());
        String value = StringUtils.trimToEmpty(row.getValue());
        List<String> newAlts = ConceptLabelBlockEditorBean.parseCsv(row.getAlts());
        List<String> previousAlts = oldAlts.getOrDefault(lang, List.of());
        if (!oldPrefs.containsKey(lang)) {
            return persistNewTranslation(ctx, lang, value, previousAlts, newAlts, dirty);
        }
        return persistExistingTranslation(ctx, lang, value, oldPrefs.get(lang), previousAlts, newAlts, dirty);
    }

    private DirtyUpdate persistNewTranslation(
            TranslationWriteContext ctx,
            String lang,
            String value,
            List<String> previousAlts,
            List<String> newAlts,
            boolean dirty
    ) {
        MutationResult added = conceptLexicalMutationService.addTranslation(
                new AddTranslationCommand(
                        ctx.thesaurusId(), ctx.conceptId(), lang, value, ctx.userId(), ctx.contributor()));
        if (!applyResult(added, dirty)) {
            return DirtyUpdate.fail();
        }
        dirty = true;
        if (!syncAlts(new TranslationAltSync(ctx, lang, previousAlts, newAlts, dirty))) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate persistExistingTranslation(
            TranslationWriteContext ctx,
            String lang,
            String value,
            String oldPref,
            List<String> previousAlts,
            List<String> newAlts,
            boolean dirty
    ) {
        if (!Strings.CS.equals(oldPref, value)) {
            MutationResult updated = conceptLexicalMutationService.updateTranslation(
                    new UpdateTranslationCommand(
                            ctx.thesaurusId(), ctx.conceptId(), lang, value, ctx.userId(), ctx.contributor()));
            if (!applyResult(updated, dirty)) {
                return DirtyUpdate.fail();
            }
            dirty = true;
        }
        if (!syncAlts(new TranslationAltSync(ctx, lang, previousAlts, newAlts, dirty))) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(dirty || !previousAlts.equals(newAlts));
    }

    private boolean syncAlts(TranslationAltSync request) {
        Set<String> oldSet = new LinkedHashSet<>(request.oldAlts());
        Set<String> newSet = new LinkedHashSet<>(request.newAlts());
        boolean dirty = request.dirty();
        for (String value : request.oldAlts()) {
            DirtyUpdate next = deleteRemovedAlt(request, value, newSet, dirty);
            if (!next.ok) {
                return false;
            }
            dirty = next.dirty;
        }
        for (String value : request.newAlts()) {
            DirtyUpdate next = addMissingAlt(request, value, oldSet, dirty);
            if (!next.ok) {
                return false;
            }
            dirty = next.dirty;
        }
        return true;
    }

    private DirtyUpdate deleteRemovedAlt(
            TranslationAltSync request, String value, Set<String> newSet, boolean dirty) {
        if (newSet.contains(value)) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult deleted = conceptLexicalMutationService.deleteSynonym(
                new DeleteSynonymCommand(
                        request.ctx().thesaurusId(), request.ctx().conceptId(), request.lang(), value,
                        request.ctx().userId(), request.ctx().contributor()));
        if (!applyResult(deleted, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate addMissingAlt(
            TranslationAltSync request, String value, Set<String> oldSet, boolean dirty) {
        if (oldSet.contains(value)) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult added = conceptLexicalMutationService.addSynonym(
                new AddSynonymCommand(
                        request.ctx().thesaurusId(), request.ctx().conceptId(), request.lang(), value, false,
                        request.ctx().userId(), request.ctx().contributor(), false));
        if (!applyResult(added, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
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
        return Strings.CS.equals(editingConceptId, detail.getSummary().getConceptId());
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
            acceptPreferredLabel(label, work, byLang);
        }
        return byLang;
    }

    private static void acceptPreferredLabel(ConceptLabel label, String work, Map<String, String> byLang) {
        if (label == null || !label.isPreferred() || StringUtils.isBlank(label.getLang())) {
            return;
        }
        String lang = normalizeLang(label.getLang());
        if (lang.equals(work)) {
            return;
        }
        byLang.putIfAbsent(lang, StringUtils.defaultString(label.getValue()));
    }

    private static Map<String, List<String>> altsByLang(ConceptDetail detail, String workLang) {
        Map<String, List<String>> byLang = new LinkedHashMap<>();
        String work = normalizeLang(workLang);
        if (detail == null || detail.getTranslations() == null) {
            return byLang;
        }
        for (ConceptLabel label : detail.getTranslations()) {
            acceptAltLabel(label, work, byLang);
        }
        return byLang;
    }

    private static void acceptAltLabel(ConceptLabel label, String work, Map<String, List<String>> byLang) {
        if (label == null || label.isPreferred() || label.isHidden() || StringUtils.isBlank(label.getLang())) {
            return;
        }
        String lang = normalizeLang(label.getLang());
        if (lang.equals(work)) {
            return;
        }
        String value = StringUtils.trimToEmpty(label.getValue());
        if (value.isEmpty()) {
            return;
        }
        byLang.computeIfAbsent(lang, key -> new ArrayList<>()).add(value);
    }

    private static String normalizeLang(String lang) {
        return StringUtils.isBlank(lang) ? "" : lang.trim().toLowerCase(Locale.ROOT);
    }

    record TranslationWriteContext(String thesaurusId, String conceptId, int userId, String contributor) {
    }

    record TranslationAltSync(
            TranslationWriteContext ctx,
            String lang,
            List<String> oldAlts,
            List<String> newAlts,
            boolean dirty
    ) {
    }
}
