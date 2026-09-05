package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteNoteType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptNoteMutationService;
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

/**
 * Édition inline du bloc Notes (type + langue + texte + source).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptNoteBlockEditorBean")
@RequiredArgsConstructor
public class ConceptNoteBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "notes";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptNoteMutationService conceptNoteMutationService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private String editingLang;
    private List<NoteBlockEditRow> rows = new ArrayList<>();
    private List<ConceptWriteNoteType> noteTypes = new ArrayList<>();
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
        return isEditing() && firstFreeCombo() != null;
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
        noteTypes = conceptWriteMetadataService.listNoteTypes();
        thesaurusLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusViewBean.getId(), editingLang);
        rows = copyRows(detail);
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
        String[] combo = firstFreeCombo();
        if (combo == null) {
            return;
        }
        rows.add(new NoteBlockEditRow(0, combo[0], combo[1], "", "", false));
    }

    public void removeRow(int index) {
        if (!isEditing() || index < 0 || index >= rows.size()) {
            return;
        }
        rows.remove(index);
    }

    public List<ConceptWriteNoteType> typesFor(NoteBlockEditRow row) {
        String lang = row == null ? "" : normalizeLang(row.getLang());
        String current = row == null ? "" : normalizeType(row.getTypeCode());
        Set<String> taken = usedTypesForLangExcluding(lang, row);
        return noteTypes.stream()
                .filter(type -> type != null && StringUtils.isNotBlank(type.code()))
                .filter(type -> {
                    String code = normalizeType(type.code());
                    return code.equals(current) || !taken.contains(code);
                })
                .toList();
    }

    public List<ConceptWriteLanguage> languagesFor(NoteBlockEditRow row) {
        String type = row == null ? "" : normalizeType(row.getTypeCode());
        String current = row == null ? "" : normalizeLang(row.getLang());
        Set<String> taken = usedLangsForTypeExcluding(type, row);
        return thesaurusLanguages.stream()
                .filter(lang -> lang != null && StringUtils.isNotBlank(lang.code()))
                .filter(lang -> {
                    String code = normalizeLang(lang.code());
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

        Map<String, NoteBlockEditRow> selected = new LinkedHashMap<>();
        Set<Integer> keptIds = new LinkedHashSet<>();
        for (NoteBlockEditRow row : rows) {
            if (row == null) {
                continue;
            }
            String type = normalizeType(row.getTypeCode());
            String lang = normalizeLang(row.getLang());
            String value = StringUtils.trimToEmpty(row.getValue());
            if (type.isEmpty()) {
                errorMessage = "Aucun type sélectionné !";
                return;
            }
            if (lang.isEmpty()) {
                errorMessage = "Aucune langue sélectionnée !";
                return;
            }
            if (value.isEmpty()) {
                errorMessage = "La note ne doit pas être vide !";
                return;
            }
            if (selected.put(comboKey(type, lang), row) != null) {
                errorMessage = "Chaque type ne peut apparaître qu'une fois par langue.";
                return;
            }
            if (row.getNoteId() > 0) {
                keptIds.add(row.getNoteId());
            }
        }

        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        Map<Integer, ConceptNote> oldById = notesById(current);
        boolean dirty = false;

        for (ConceptNote old : oldById.values()) {
            int noteId = parseNoteId(old.id());
            if (noteId <= 0 || keptIds.contains(noteId)) {
                continue;
            }
            MutationResult deleted = conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                    thesaurusId, conceptId, noteId, old.lang(), old.typeCode(), userId, contributor));
            if (!applyResult(deleted, dirty)) {
                return;
            }
            dirty = true;
        }

        for (NoteBlockEditRow row : selected.values()) {
            String type = normalizeType(row.getTypeCode());
            String lang = normalizeLang(row.getLang());
            String value = StringUtils.trimToEmpty(row.getValue());
            String source = StringUtils.trimToEmpty(row.getSource());
            ConceptNote previous = row.getNoteId() > 0 ? oldById.get(row.getNoteId()) : null;
            if (previous != null
                    && StringUtils.equals(StringUtils.trimToEmpty(previous.value()), value)
                    && StringUtils.equals(StringUtils.trimToEmpty(previous.source()), source)) {
                continue;
            }
            MutationResult upserted = conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                    thesaurusId, conceptId, lang, type, value, source, userId, contributor));
            if (!applyResult(upserted, dirty)) {
                return;
            }
            dirty = true;
        }

        finishSuccess();
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
        flashMessage = "Notes enregistrées";
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
        noteTypes = new ArrayList<>();
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

    private String[] firstFreeCombo() {
        Set<String> taken = usedCombosExcluding(null);
        for (ConceptWriteLanguage lang : languagesWorkFirst()) {
            if (lang == null || StringUtils.isBlank(lang.code())) {
                continue;
            }
            for (ConceptWriteNoteType type : noteTypes) {
                if (type == null || StringUtils.isBlank(type.code())) {
                    continue;
                }
                if (!taken.contains(comboKey(type.code(), lang.code()))) {
                    return new String[] { type.code(), lang.code() };
                }
            }
        }
        return null;
    }

    private List<ConceptWriteLanguage> languagesWorkFirst() {
        String work = normalizeLang(editingLang);
        List<ConceptWriteLanguage> ordered = new ArrayList<>();
        for (ConceptWriteLanguage lang : thesaurusLanguages) {
            if (lang != null && work.equals(normalizeLang(lang.code()))) {
                ordered.add(lang);
            }
        }
        for (ConceptWriteLanguage lang : thesaurusLanguages) {
            if (lang != null && !work.equals(normalizeLang(lang.code()))) {
                ordered.add(lang);
            }
        }
        return ordered;
    }

    private Set<String> usedCombosExcluding(NoteBlockEditRow skip) {
        Set<String> taken = new LinkedHashSet<>();
        for (NoteBlockEditRow row : rows) {
            if (row == skip) {
                continue;
            }
            String type = normalizeType(row.getTypeCode());
            String lang = normalizeLang(row.getLang());
            if (!type.isEmpty() && !lang.isEmpty()) {
                taken.add(comboKey(type, lang));
            }
        }
        return taken;
    }

    private Set<String> usedTypesForLangExcluding(String lang, NoteBlockEditRow skip) {
        Set<String> taken = new LinkedHashSet<>();
        if (lang.isEmpty()) {
            return taken;
        }
        for (NoteBlockEditRow row : rows) {
            if (row == skip || !lang.equals(normalizeLang(row.getLang()))) {
                continue;
            }
            String type = normalizeType(row.getTypeCode());
            if (!type.isEmpty()) {
                taken.add(type);
            }
        }
        return taken;
    }

    private Set<String> usedLangsForTypeExcluding(String type, NoteBlockEditRow skip) {
        Set<String> taken = new LinkedHashSet<>();
        if (type.isEmpty()) {
            return taken;
        }
        for (NoteBlockEditRow row : rows) {
            if (row == skip || !type.equals(normalizeType(row.getTypeCode()))) {
                continue;
            }
            String lang = normalizeLang(row.getLang());
            if (!lang.isEmpty()) {
                taken.add(lang);
            }
        }
        return taken;
    }

    private String resolveWorkLang(ConceptDetail detail) {
        if (detail != null && detail.getSummary() != null
                && StringUtils.isNotBlank(detail.getSummary().getLang())) {
            return detail.getSummary().getLang();
        }
        return StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
    }

    private static List<NoteBlockEditRow> copyRows(ConceptDetail detail) {
        List<NoteBlockEditRow> copied = new ArrayList<>();
        if (detail == null || detail.getNotes() == null) {
            return copied;
        }
        for (ConceptNote note : detail.getNotes()) {
            if (note == null || StringUtils.isBlank(note.typeCode()) || StringUtils.isBlank(note.lang())) {
                continue;
            }
            copied.add(new NoteBlockEditRow(
                    parseNoteId(note.id()),
                    note.typeCode(),
                    note.lang(),
                    StringUtils.defaultString(note.value()),
                    StringUtils.defaultString(note.source()),
                    true));
        }
        return copied;
    }

    private static Map<Integer, ConceptNote> notesById(ConceptDetail detail) {
        Map<Integer, ConceptNote> byId = new LinkedHashMap<>();
        if (detail == null || detail.getNotes() == null) {
            return byId;
        }
        for (ConceptNote note : detail.getNotes()) {
            int noteId = parseNoteId(note == null ? null : note.id());
            if (noteId > 0) {
                byId.putIfAbsent(noteId, note);
            }
        }
        return byId;
    }

    private static int parseNoteId(String id) {
        if (StringUtils.isBlank(id)) {
            return 0;
        }
        try {
            return Integer.parseInt(id.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String comboKey(String type, String lang) {
        return normalizeType(type) + "\0" + normalizeLang(lang);
    }

    private static String normalizeType(String type) {
        return StringUtils.trimToEmpty(type);
    }

    private static String normalizeLang(String lang) {
        return StringUtils.isBlank(lang) ? "" : lang.trim().toLowerCase(Locale.ROOT);
    }
}
