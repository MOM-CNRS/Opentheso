package fr.cnrs.opentheso.v2.concept.write.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLexicalMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptLifecycleMutationService;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.service.FacetMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Édition inline du bloc Libellé (préf. + formes alternatives / cachées + facettes) sans dialogue.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptLabelBlockEditorBean")
@RequiredArgsConstructor
public class ConceptLabelBlockEditorBean implements Serializable {

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptLifecycleMutationService conceptLifecycleMutationService;
    private final transient ConceptLexicalMutationService conceptLexicalMutationService;
    private final transient FacetMutationService facetMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ThesaurusContext thesaurusContext;
    private final transient ConceptSelectionContext conceptSelectionContext;

    static final String FICHE_CARD = "contexte";

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private String editingLang;
    private String preferredLabel;
    private String altLabels;
    private String hiddenLabels;
    private List<FacetEditRow> selectedFacets = new ArrayList<>();
    private boolean duplicateWarning;
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

    public void startEditing() {
        if (!isEditable()) {
            return;
        }
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return;
        }
        editingConceptId = detail.getSummary().getConceptId();
        editingLang = resolveLang(detail);
        preferredLabel = StringUtils.defaultString(detail.getSummary().getPreferredLabel());
        altLabels = joinCsv(detail.getSynonyms());
        hiddenLabels = joinCsv(detail.getHiddenSynonyms());
        selectedFacets = copyFacets(detail.getFacets());
        duplicateWarning = false;
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

    public String getSelectedFacetsJson() {
        return toFacetsJson(selectedFacets);
    }

    public void setSelectedFacetsJson(String json) {
        List<FacetEditRow> parsed = parseFacetsJson(json);
        if (parsed != null) {
            selectedFacets = parsed;
        }
    }

    public void save() {
        saveInternal(false);
    }

    public void saveForced() {
        saveInternal(true);
    }

    private void saveInternal(boolean forced) {
        duplicateWarning = false;
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
        String pref = StringUtils.trimToEmpty(preferredLabel);
        if (pref.isEmpty()) {
            errorMessage = "Le libellé est obligatoire.";
            return;
        }
        List<String> newAlts = parseCsv(altLabels);
        List<String> newHidden = parseCsv(hiddenLabels);
        if (hasOverlap(newAlts, newHidden)) {
            errorMessage = "Une forme ne peut pas être à la fois alternative et cachée.";
            return;
        }

        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String lang = resolveLang(current);
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        String currentPref = StringUtils.trimToEmpty(current.getSummary().getPreferredLabel());
        boolean dirty = false;

        if (!StringUtils.equals(pref, currentPref)) {
            MutationResult renamed = conceptLifecycleMutationService.renamePreferredLabel(
                    new RenamePreferredLabelCommand(
                            thesaurusId,
                            conceptId,
                            lang,
                            userId,
                            contributor,
                            pref,
                            "",
                            forced
                    ));
            if (!applyResult(renamed, dirty)) {
                return;
            }
            dirty = true;
        }

        List<String> oldAlts = orEmpty(current.getSynonyms());
        List<String> oldHidden = orEmpty(current.getHiddenSynonyms());
        Set<String> oldAltSet = new LinkedHashSet<>(oldAlts);
        Set<String> oldHiddenSet = new LinkedHashSet<>(oldHidden);
        Set<String> newAltSet = new LinkedHashSet<>(newAlts);
        Set<String> newHiddenSet = new LinkedHashSet<>(newHidden);
        Set<String> oldAll = union(oldAltSet, oldHiddenSet);
        Set<String> newAll = union(newAltSet, newHiddenSet);

        for (String value : oldAll) {
            if (!newAll.contains(value)) {
                MutationResult deleted = conceptLexicalMutationService.deleteSynonym(new DeleteSynonymCommand(
                        thesaurusId, conceptId, lang, value, userId, contributor));
                if (!applyResult(deleted, dirty)) {
                    return;
                }
                dirty = true;
            }
        }
        for (String value : oldAltSet) {
            if (newHiddenSet.contains(value) && !newAltSet.contains(value)) {
                MutationResult updated = conceptLexicalMutationService.updateSynonym(new UpdateSynonymCommand(
                        thesaurusId, conceptId, lang, value, value, true, userId, contributor, forced));
                if (!applyResult(updated, dirty)) {
                    return;
                }
                dirty = true;
            }
        }
        for (String value : oldHiddenSet) {
            if (newAltSet.contains(value) && !newHiddenSet.contains(value)) {
                MutationResult updated = conceptLexicalMutationService.updateSynonym(new UpdateSynonymCommand(
                        thesaurusId, conceptId, lang, value, value, false, userId, contributor, forced));
                if (!applyResult(updated, dirty)) {
                    return;
                }
                dirty = true;
            }
        }
        for (String value : newAlts) {
            if (!oldAll.contains(value)) {
                MutationResult added = conceptLexicalMutationService.addSynonym(new AddSynonymCommand(
                        thesaurusId, conceptId, lang, value, false, userId, contributor, forced));
                if (!applyResult(added, dirty)) {
                    return;
                }
                dirty = true;
            }
        }
        for (String value : newHidden) {
            if (!oldAll.contains(value)) {
                MutationResult added = conceptLexicalMutationService.addSynonym(new AddSynonymCommand(
                        thesaurusId, conceptId, lang, value, true, userId, contributor, forced));
                if (!applyResult(added, dirty)) {
                    return;
                }
                dirty = true;
            }
        }

        Set<String> oldFacetIds = current.getFacets() == null
                ? Set.of()
                : current.getFacets().stream()
                        .map(ConceptRelation::getConceptId)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> newFacetIds = selectedFacetIds();
        for (String facetId : oldFacetIds) {
            if (!newFacetIds.contains(facetId)) {
                MutationResult removed = facetMutationService.removeMember(
                        new RemoveFacetMemberCommand(thesaurusId, facetId, conceptId, false));
                if (!applyResult(removed, dirty)) {
                    return;
                }
                dirty = true;
            }
        }
        for (FacetEditRow row : selectedFacets) {
            if (row == null || StringUtils.isBlank(row.getId()) || oldFacetIds.contains(row.getId())) {
                continue;
            }
            MutationResult added = facetMutationService.addMember(
                    new AddFacetMemberCommand(thesaurusId, row.getId(), conceptId, false));
            if (!applyResult(added, dirty)) {
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
        if (result.outcome() == MutationOutcome.DUPLICATE_LABEL) {
            duplicateWarning = true;
            errorMessage = StringUtils.defaultIfBlank(
                    result.message(), "Ce libellé existe déjà. Enregistrer quand même ?");
            reloadIfDirty(dirty);
            return false;
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
        duplicateWarning = false;
        errorMessage = "";
        flashMessage = "Libellé enregistré";
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
        preferredLabel = "";
        altLabels = "";
        hiddenLabels = "";
        selectedFacets = new ArrayList<>();
        duplicateWarning = false;
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
        return StringUtils.equals(editingConceptId, detail.getSummary().getConceptId())
                && StringUtils.equals(editingLang, resolveLang(detail));
    }

    private String resolveLang(ConceptDetail detail) {
        String lang = detail.getSummary() == null ? null : detail.getSummary().getLang();
        if (StringUtils.isNotBlank(lang)) {
            return lang;
        }
        lang = thesaurusViewBean.getSelectedLang();
        if (StringUtils.isNotBlank(lang)) {
            return lang;
        }
        return thesaurusContext.resolveWorkLanguage();
    }

    private static final ObjectMapper FACET_JSON = new ObjectMapper();

    static String toFacetsJson(List<FacetEditRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (FacetEditRow row : rows) {
            if (row == null || StringUtils.isBlank(row.getId())) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"id\":").append(jsonQuote(row.getId()))
                    .append(",\"label\":").append(jsonQuote(row.getDisplayLabel()))
                    .append('}');
        }
        return sb.append(']').toString();
    }

    static List<FacetEditRow> parseFacetsJson(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "[]".equals(trimmed)) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = FACET_JSON.readTree(trimmed);
            if (!root.isArray()) {
                return null;
            }
            List<FacetEditRow> rows = new ArrayList<>();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (JsonNode node : root) {
                if (node == null || !node.isObject()) {
                    continue;
                }
                String id = node.path("id").asText("");
                if (StringUtils.isBlank(id) || !seen.add(id)) {
                    continue;
                }
                String label = node.path("label").asText("");
                rows.add(new FacetEditRow(id, label));
            }
            return rows;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String jsonQuote(String value) {
        String escaped = StringUtils.defaultString(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "\"" + escaped + "\"";
    }

    private Set<String> selectedFacetIds() {
        return selectedFacets.stream()
                .map(FacetEditRow::getId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<FacetEditRow> copyFacets(List<ConceptRelation> facets) {
        if (facets == null || facets.isEmpty()) {
            return new ArrayList<>();
        }
        List<FacetEditRow> rows = new ArrayList<>();
        for (ConceptRelation facet : facets) {
            if (facet == null || StringUtils.isBlank(facet.getConceptId())) {
                continue;
            }
            rows.add(new FacetEditRow(facet.getConceptId(), facet.getDisplayLabel()));
        }
        return rows;
    }

    static List<String> parseCsv(String raw) {
        if (StringUtils.isBlank(raw)) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = StringUtils.trimToEmpty(part);
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return List.copyOf(values);
    }

    static String joinCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));
    }

    private static List<String> orEmpty(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            String trimmed = StringUtils.trimToEmpty(value);
            if (!trimmed.isEmpty()) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }

    private static boolean hasOverlap(List<String> alts, List<String> hidden) {
        Set<String> altSet = new LinkedHashSet<>(alts);
        for (String value : hidden) {
            if (altSet.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        LinkedHashSet<String> all = new LinkedHashSet<>(left);
        all.addAll(right);
        return all;
    }
}
