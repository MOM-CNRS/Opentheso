package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptNote;
import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
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
import fr.cnrs.opentheso.v2.concept.write.ui.NoteBlockEditRow;
import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Édition inline des notes de la fiche facette.
 */
@Getter
@Setter
@ViewScoped
@Named("v2FacetNoteBlockEditorBean")
@RequiredArgsConstructor
public class FacetNoteBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "f-notes";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptNoteMutationService conceptNoteMutationService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingFacetId;
    private String editingLang;
    private List<NoteBlockEditRow> rows = new ArrayList<>();
    private List<ConceptWriteNoteType> noteTypes = new ArrayList<>();
    private List<ConceptWriteLanguage> thesaurusLanguages = new ArrayList<>();
    private String notesPayload = "";
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedFacet() != null
                && conceptWritePolicy.canMutateLexicalContent(userSession, false);
    }

    public boolean isEditing() {
        if (editing && !matchesCurrentFacet()) {
            resetForm(false);
        }
        return editing && FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard());
    }

    public List<ConceptWriteLanguage> getPickerLanguages() {
        List<ConceptWriteLanguage> langs = thesaurusLanguages == null
                ? List.of()
                : thesaurusLanguages;
        return langs.stream()
                .filter(lang -> lang != null && StringUtils.isNotBlank(lang.code()))
                .toList();
    }

    public void startEditing() {
        if (!isEditable()) {
            return;
        }
        FacetDetailOverview facet = thesaurusViewBean.getSelectedFacet();
        if (facet == null || StringUtils.isBlank(facet.facetId())) {
            return;
        }
        editingFacetId = facet.facetId();
        editingLang = resolveWorkLang(facet);
        noteTypes = conceptWriteMetadataService.listNoteTypes();
        if (noteTypes == null || noteTypes.isEmpty()) {
            noteTypes = List.of(
                    new ConceptWriteNoteType("scopeNote"),
                    new ConceptWriteNoteType("example"),
                    new ConceptWriteNoteType("historyNote"),
                    new ConceptWriteNoteType("editorialNote"),
                    new ConceptWriteNoteType("changeNote")
            );
        }
        thesaurusLanguages = conceptWriteMetadataService.listUsedLanguages(
                thesaurusViewBean.getId(), editingLang);
        rows = copyRows(facet);
        notesPayload = serializeRows(rows);
        errorMessage = "";
        flashMessage = "";
        flashToken = "";
        editing = true;
        thesaurusViewBean.setFicheEditCard(FICHE_CARD);
    }

    public void cancel() {
        resetForm(false);
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
        FacetDetailOverview current = thesaurusViewBean.getSelectedFacet();
        if (current == null || StringUtils.isBlank(current.facetId())) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        applyPayloadToRows();
        Map<String, NoteBlockEditRow> selected = new LinkedHashMap<>();
        Set<Integer> keptIds = new LinkedHashSet<>();
        for (NoteBlockEditRow row : rows) {
            if (!acceptSelectedNote(row, selected, keptIds)) {
                return;
            }
        }
        persistNoteChanges(current, selected, keptIds, userId);
    }

    private boolean acceptSelectedNote(
            NoteBlockEditRow row, Map<String, NoteBlockEditRow> selected, Set<Integer> keptIds) {
        if (row == null) {
            return true;
        }
        String type = normalizeType(row.getTypeCode());
        String lang = normalizeLang(row.getLang());
        String value = StringUtils.trimToEmpty(row.getValue());
        if (value.isEmpty()) {
            return true;
        }
        if (type.isEmpty()) {
            errorMessage = "Aucun type sélectionné !";
            return false;
        }
        if (lang.isEmpty()) {
            errorMessage = "Aucune langue sélectionnée !";
            return false;
        }
        if (selected.put(comboKey(type, lang), row) != null) {
            errorMessage = "Chaque type ne peut apparaître qu'une fois par langue.";
            return false;
        }
        if (row.getNoteId() > 0) {
            keptIds.add(row.getNoteId());
        }
        return true;
    }

    private void persistNoteChanges(
            FacetDetailOverview current,
            Map<String, NoteBlockEditRow> selected,
            Set<Integer> keptIds,
            int userId
    ) {
        String thesaurusId = thesaurusViewBean.getId();
        String facetId = current.facetId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        Map<Integer, ConceptNote> oldById = notesById(current);
        boolean dirty = false;
        for (ConceptNote old : oldById.values()) {
            int noteId = parseNoteId(old.id());
            if (noteId <= 0 || keptIds.contains(noteId)) {
                continue;
            }
            MutationResult deleted = conceptNoteMutationService.deleteNote(new DeleteNoteCommand(
                    thesaurusId, facetId, noteId, old.lang(), old.typeCode(), userId, contributor));
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
                    && Strings.CS.equals(StringUtils.trimToEmpty(previous.value()), value)
                    && Strings.CS.equals(StringUtils.trimToEmpty(previous.source()), source)
                    && Strings.CS.equals(normalizeType(previous.typeCode()), type)
                    && Strings.CS.equals(normalizeLang(previous.lang()), lang)) {
                continue;
            }
            MutationResult upserted = conceptNoteMutationService.upsertNote(new UpsertNoteCommand(
                    thesaurusId, facetId, lang, type, value, source, userId, contributor));
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
        editingFacetId = null;
        editingLang = null;
        rows = new ArrayList<>();
        noteTypes = new ArrayList<>();
        thesaurusLanguages = new ArrayList<>();
        notesPayload = "";
        errorMessage = "";
        if (!keepFlash) {
            flashMessage = "";
            flashToken = "";
        }
    }

    private boolean matchesCurrentFacet() {
        FacetDetailOverview facet = thesaurusViewBean.getSelectedFacet();
        return facet != null && Strings.CS.equals(editingFacetId, facet.facetId());
    }

    private String resolveWorkLang(FacetDetailOverview facet) {
        if (facet != null && StringUtils.isNotBlank(facet.lang())) {
            return facet.lang();
        }
        return StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
    }

    private static List<NoteBlockEditRow> copyRows(FacetDetailOverview facet) {
        List<NoteBlockEditRow> copied = new ArrayList<>();
        if (facet == null || facet.notes() == null) {
            return copied;
        }
        for (ConceptNote note : facet.notes()) {
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

    private static Map<Integer, ConceptNote> notesById(FacetDetailOverview facet) {
        Map<Integer, ConceptNote> byId = new LinkedHashMap<>();
        if (facet == null || facet.notes() == null) {
            return byId;
        }
        for (ConceptNote note : facet.notes()) {
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

    static String serializeRows(List<NoteBlockEditRow> source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (NoteBlockEditRow row : source) {
            if (row == null) {
                continue;
            }
            String type = normalizeType(row.getTypeCode());
            String lang = normalizeLang(row.getLang());
            if (type.isEmpty() || lang.isEmpty()) {
                continue;
            }
            lines.add(type + "\t" + lang + "\t"
                    + encodeNotePart(row.getValue()) + "\t"
                    + encodeNotePart(row.getSource()));
        }
        return String.join("\n", lines);
    }

    void applyPayloadToRows() {
        Map<String, NoteBlockEditRow> previous = new LinkedHashMap<>();
        for (NoteBlockEditRow row : rows) {
            if (row == null) {
                continue;
            }
            String type = normalizeType(row.getTypeCode());
            String lang = normalizeLang(row.getLang());
            if (!type.isEmpty() && !lang.isEmpty()) {
                previous.putIfAbsent(comboKey(type, lang), row);
            }
        }
        List<NoteBlockEditRow> parsed = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String line : StringUtils.defaultString(notesPayload).split("\\R")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String[] parts = line.split("\\t", 4);
            String type = parts.length > 0 ? normalizeType(parts[0]) : "";
            String lang = parts.length > 1 ? normalizeLang(parts[1]) : "";
            String value = parts.length > 2 ? decodeNotePart(parts[2]) : "";
            String source = parts.length > 3 ? decodeNotePart(parts[3]) : "";
            if (type.isEmpty() || lang.isEmpty() || value.isEmpty() || !seen.add(comboKey(type, lang))) {
                continue;
            }
            NoteBlockEditRow old = previous.get(comboKey(type, lang));
            parsed.add(new NoteBlockEditRow(
                    old == null ? 0 : old.getNoteId(),
                    type,
                    lang,
                    value,
                    source,
                    old != null && old.isExisting()));
        }
        rows = parsed;
    }

    private static String encodeNotePart(String raw) {
        return URLEncoder.encode(StringUtils.defaultString(raw), StandardCharsets.UTF_8);
    }

    private static String decodeNotePart(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        try {
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return raw;
        }
    }

    private static String comboKey(String type, String lang) {
        return normalizeType(type) + "\t" + normalizeLang(lang);
    }

    private static String normalizeType(String type) {
        return StringUtils.isBlank(type) ? "" : type.trim();
    }

    private static String normalizeLang(String lang) {
        return StringUtils.isBlank(lang) ? "" : lang.trim().toLowerCase(Locale.ROOT);
    }
}
