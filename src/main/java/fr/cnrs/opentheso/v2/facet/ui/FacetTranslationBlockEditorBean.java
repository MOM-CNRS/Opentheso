package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.GroupTranslationItem;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteLanguage;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.concept.write.ui.TranslationBlockEditRow;
import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.DeleteFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.UpdateFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
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

/**
 * Édition inline du bloc Traductions de la fiche facette (hors langue de travail).
 */
@Getter
@Setter
@ViewScoped
@Named("v2FacetTranslationBlockEditorBean")
@RequiredArgsConstructor
public class FacetTranslationBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "f-tr";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient FacetMutationService facetMutationService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingFacetId;
    private String editingLang;
    private List<TranslationBlockEditRow> rows = new ArrayList<>();
    private List<ConceptWriteLanguage> thesaurusLanguages = new ArrayList<>();
    private String translationsPayload = "";
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
        List<ConceptWriteLanguage> langs = conceptWriteMetadataService.listUsedLanguages(
                thesaurusViewBean.getId(), editingLang);
        thesaurusLanguages = langs == null ? new ArrayList<>() : langs;
        rows = copyRows(facet, editingLang);
        translationsPayload = "";
        errorMessage = "";
        flashMessage = "";
        flashToken = "";
        editing = true;
        thesaurusViewBean.setFicheEditCard(FICHE_CARD);
    }

    public void cancel() {
        resetForm(false);
    }

    public List<ConceptWriteLanguage> getPickerLanguages() {
        String work = normalizeLang(editingLang);
        List<ConceptWriteLanguage> langs = thesaurusLanguages == null
                ? List.of()
                : thesaurusLanguages;
        return langs.stream()
                .filter(lang -> lang != null && StringUtils.isNotBlank(lang.code()))
                .filter(lang -> !work.equals(normalizeLang(lang.code())))
                .toList();
    }

    public void save() {
        errorMessage = "";
        if (!isEditable() || !isEditing()) {
            return;
        }
        if (userSession.getCurrentUserId() == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        FacetDetailOverview current = thesaurusViewBean.getSelectedFacet();
        if (current == null || StringUtils.isBlank(current.facetId())) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        if (StringUtils.isNotBlank(translationsPayload)) {
            applyPayloadToRows();
        }
        String workLang = resolveWorkLang(current);
        Optional<Map<String, String>> selected = collectSelectedTranslations(workLang);
        if (selected.isEmpty()) {
            return;
        }
        persistTranslationChanges(current, workLang, selected.get());
    }

    private Optional<Map<String, String>> collectSelectedTranslations(String workLang) {
        Map<String, String> selected = new LinkedHashMap<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (TranslationBlockEditRow row : rows) {
            if (row == null) {
                continue;
            }
            String lang = normalizeLang(row.getLang());
            String value = StringUtils.trimToEmpty(row.getValue());
            if (lang.isEmpty()) {
                errorMessage = "Aucune langue sélectionnée !";
                return Optional.empty();
            }
            if (lang.equals(normalizeLang(workLang))) {
                errorMessage = "La langue de travail s'édite dans le bloc Libellé.";
                return Optional.empty();
            }
            if (!seen.add(lang)) {
                errorMessage = "Chaque langue ne peut apparaître qu'une fois.";
                return Optional.empty();
            }
            if (value.isEmpty()) {
                continue;
            }
            selected.put(lang, value);
        }
        return Optional.of(selected);
    }

    private void persistTranslationChanges(
            FacetDetailOverview current,
            String workLang,
            Map<String, String> selected
    ) {
        String thesaurusId = thesaurusViewBean.getId();
        String facetId = current.facetId();
        Map<String, String> oldPrefs = preferredByLang(current, workLang);
        boolean dirty = false;
        for (String lang : oldPrefs.keySet()) {
            if (selected.containsKey(lang)) {
                continue;
            }
            MutationResult deleted = facetMutationService.deleteTranslation(
                    new DeleteFacetTranslationCommand(thesaurusId, facetId, lang));
            if (!applyResult(deleted, dirty)) {
                return;
            }
            dirty = true;
        }
        for (Map.Entry<String, String> entry : selected.entrySet()) {
            String lang = entry.getKey();
            String value = entry.getValue();
            MutationResult result;
            if (!oldPrefs.containsKey(lang)) {
                result = facetMutationService.addTranslation(
                        new AddFacetTranslationCommand(thesaurusId, facetId, lang, value));
            } else if (!Strings.CS.equals(oldPrefs.get(lang), value)) {
                result = facetMutationService.updateTranslation(
                        new UpdateFacetTranslationCommand(thesaurusId, facetId, lang, value));
            } else {
                continue;
            }
            if (!applyResult(result, dirty)) {
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
        flashMessage = "Traductions enregistrées";
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
        thesaurusLanguages = new ArrayList<>();
        translationsPayload = "";
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

    private static List<TranslationBlockEditRow> copyRows(FacetDetailOverview facet, String workLang) {
        List<TranslationBlockEditRow> copied = new ArrayList<>();
        for (Map.Entry<String, String> entry : preferredByLang(facet, workLang).entrySet()) {
            copied.add(new TranslationBlockEditRow(entry.getKey(), entry.getValue(), "", true));
        }
        return copied;
    }

    private static Map<String, String> preferredByLang(FacetDetailOverview facet, String workLang) {
        Map<String, String> byLang = new LinkedHashMap<>();
        String work = normalizeLang(workLang);
        if (facet == null || facet.translations() == null) {
            return byLang;
        }
        for (GroupTranslationItem translation : facet.translations()) {
            if (translation == null || StringUtils.isBlank(translation.lang())) {
                continue;
            }
            String lang = normalizeLang(translation.lang());
            if (lang.equals(work) || byLang.containsKey(lang)) {
                continue;
            }
            byLang.put(lang, StringUtils.defaultString(translation.value()));
        }
        return byLang;
    }

    void applyPayloadToRows() {
        Map<String, Boolean> existingByLang = new LinkedHashMap<>();
        for (TranslationBlockEditRow row : rows) {
            if (row == null || StringUtils.isBlank(row.getLang())) {
                continue;
            }
            existingByLang.put(normalizeLang(row.getLang()), row.isExisting());
        }
        List<TranslationBlockEditRow> parsed = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String line : StringUtils.defaultString(translationsPayload).split("\\R")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String[] parts = line.split("\\t", 2);
            String lang = normalizeLang(parts[0]);
            if (lang.isEmpty() || !seen.add(lang)) {
                continue;
            }
            String value = parts.length > 1 ? parts[1] : "";
            parsed.add(new TranslationBlockEditRow(
                    lang, value, "", existingByLang.getOrDefault(lang, false)));
        }
        rows = parsed;
    }

    private static String normalizeLang(String lang) {
        return StringUtils.isBlank(lang) ? "" : lang.trim().toLowerCase(Locale.ROOT);
    }
}
