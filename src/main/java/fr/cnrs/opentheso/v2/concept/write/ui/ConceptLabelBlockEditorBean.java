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
import org.apache.commons.lang3.Strings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
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
        parseFacetsJson(json).ifPresent(parsed -> selectedFacets = parsed);
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
        if (!prepareLabelSave()) {
            return;
        }
        persistLabelChanges(forced);
    }

    private boolean prepareLabelSave() {
        if (!isEditable() || !isEditing()) {
            return false;
        }
        if (userSession.getCurrentUserId() == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return false;
        }
        ConceptDetail current = thesaurusViewBean.getSelectedConcept();
        if (current == null || current.getSummary() == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return false;
        }
        if (StringUtils.trimToEmpty(preferredLabel).isEmpty()) {
            errorMessage = "Le libellé est obligatoire.";
            return false;
        }
        if (hasOverlap(parseCsv(altLabels), parseCsv(hiddenLabels))) {
            errorMessage = "Une forme ne peut pas être à la fois alternative et cachée.";
            return false;
        }
        return true;
    }

    private void persistLabelChanges(boolean forced) {
        ConceptDetail current = thesaurusViewBean.getSelectedConcept();
        LabelWriteContext ctx = new LabelWriteContext(
                thesaurusViewBean.getId(),
                current.getSummary().getConceptId(),
                resolveLang(current),
                userSession.getCurrentUserId(),
                StringUtils.defaultString(userSession.getCurrentUsername()),
                forced);
        DirtyUpdate dirty = renamePreferredIfNeeded(current, ctx, false);
        if (!dirty.ok) {
            return;
        }
        dirty = syncSynonyms(current, ctx, dirty.dirty);
        if (!dirty.ok) {
            return;
        }
        if (!syncFacets(current, ctx, dirty.dirty).ok) {
            return;
        }
        finishSuccess();
    }

    private DirtyUpdate renamePreferredIfNeeded(ConceptDetail current, LabelWriteContext ctx, boolean dirty) {
        String pref = StringUtils.trimToEmpty(preferredLabel);
        String currentPref = StringUtils.trimToEmpty(current.getSummary().getPreferredLabel());
        if (Strings.CS.equals(pref, currentPref)) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult renamed = conceptLifecycleMutationService.renamePreferredLabel(
                new RenamePreferredLabelCommand(
                        ctx.thesaurusId(),
                        ctx.conceptId(),
                        ctx.lang(),
                        ctx.userId(),
                        ctx.contributor(),
                        pref,
                        "",
                        ctx.forced()));
        if (!applyResult(renamed, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate syncSynonyms(ConceptDetail current, LabelWriteContext ctx, boolean dirty) {
        SynonymSets sets = SynonymSets.from(
                orEmpty(current.getSynonyms()),
                orEmpty(current.getHiddenSynonyms()),
                parseCsv(altLabels),
                parseCsv(hiddenLabels));
        DirtyUpdate next = deleteRemovedSynonyms(ctx, sets, dirty);
        if (!next.ok) {
            return DirtyUpdate.fail();
        }
        dirty = next.dirty;
        next = hideExistingAlts(ctx, sets, dirty);
        if (!next.ok) {
            return DirtyUpdate.fail();
        }
        dirty = next.dirty;
        next = unhideExistingHidden(ctx, sets, dirty);
        if (!next.ok) {
            return DirtyUpdate.fail();
        }
        dirty = next.dirty;
        next = addMissingAlts(ctx, sets, dirty);
        if (!next.ok) {
            return DirtyUpdate.fail();
        }
        return addMissingHidden(ctx, sets, next.dirty);
    }

    private DirtyUpdate deleteRemovedSynonyms(LabelWriteContext ctx, SynonymSets sets, boolean dirty) {
        for (String value : sets.oldAll()) {
            DirtyUpdate next = deleteRemovedSynonym(ctx, value, sets.newAll(), dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate deleteRemovedSynonym(LabelWriteContext ctx, String value, Set<String> newAll, boolean dirty) {
        if (newAll.contains(value)) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult deleted = conceptLexicalMutationService.deleteSynonym(new DeleteSynonymCommand(
                ctx.thesaurusId(), ctx.conceptId(), ctx.lang(), value, ctx.userId(), ctx.contributor()));
        if (!applyResult(deleted, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate hideExistingAlts(LabelWriteContext ctx, SynonymSets sets, boolean dirty) {
        for (String value : sets.oldAltSet()) {
            DirtyUpdate next = hideExistingAlt(ctx, value, sets, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate hideExistingAlt(LabelWriteContext ctx, String value, SynonymSets sets, boolean dirty) {
        if (!sets.newHiddenSet().contains(value) || sets.newAltSet().contains(value)) {
            return DirtyUpdate.of(dirty);
        }
        return applySynonymUpdate(ctx, value, true, dirty);
    }

    private DirtyUpdate unhideExistingHidden(LabelWriteContext ctx, SynonymSets sets, boolean dirty) {
        for (String value : sets.oldHiddenSet()) {
            DirtyUpdate next = unhideExistingHiddenValue(ctx, value, sets, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate unhideExistingHiddenValue(LabelWriteContext ctx, String value, SynonymSets sets, boolean dirty) {
        if (!sets.newAltSet().contains(value) || sets.newHiddenSet().contains(value)) {
            return DirtyUpdate.of(dirty);
        }
        return applySynonymUpdate(ctx, value, false, dirty);
    }

    private DirtyUpdate applySynonymUpdate(LabelWriteContext ctx, String value, boolean hidden, boolean dirty) {
        MutationResult updated = conceptLexicalMutationService.updateSynonym(new UpdateSynonymCommand(
                ctx.thesaurusId(), ctx.conceptId(), ctx.lang(), value, value, hidden,
                ctx.userId(), ctx.contributor(), ctx.forced()));
        if (!applyResult(updated, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate addMissingAlts(LabelWriteContext ctx, SynonymSets sets, boolean dirty) {
        for (String value : sets.newAlts()) {
            DirtyUpdate next = addMissingSynonym(ctx, value, false, sets.oldAll(), dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate addMissingHidden(LabelWriteContext ctx, SynonymSets sets, boolean dirty) {
        for (String value : sets.newHidden()) {
            DirtyUpdate next = addMissingSynonym(ctx, value, true, sets.oldAll(), dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate addMissingSynonym(
            LabelWriteContext ctx, String value, boolean hidden, Set<String> oldAll, boolean dirty) {
        if (oldAll.contains(value)) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult added = conceptLexicalMutationService.addSynonym(new AddSynonymCommand(
                ctx.thesaurusId(), ctx.conceptId(), ctx.lang(), value, hidden,
                ctx.userId(), ctx.contributor(), ctx.forced()));
        if (!applyResult(added, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate syncFacets(ConceptDetail current, LabelWriteContext ctx, boolean dirty) {
        Set<String> oldFacetIds = current.getFacets() == null
                ? Set.of()
                : current.getFacets().stream()
                        .map(ConceptRelation::getConceptId)
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        DirtyUpdate next = removeUnselectedFacets(ctx, oldFacetIds, selectedFacetIds(), dirty);
        if (!next.ok) {
            return DirtyUpdate.fail();
        }
        return addSelectedFacets(ctx, oldFacetIds, next.dirty);
    }

    private DirtyUpdate removeUnselectedFacets(
            LabelWriteContext ctx, Set<String> oldFacetIds, Set<String> newFacetIds, boolean dirty) {
        for (String facetId : oldFacetIds) {
            DirtyUpdate next = removeUnselectedFacet(ctx, facetId, newFacetIds, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate removeUnselectedFacet(
            LabelWriteContext ctx, String facetId, Set<String> newFacetIds, boolean dirty) {
        if (newFacetIds.contains(facetId)) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult removed = facetMutationService.removeMember(
                new RemoveFacetMemberCommand(ctx.thesaurusId(), facetId, ctx.conceptId(), false));
        if (!applyResult(removed, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate addSelectedFacets(LabelWriteContext ctx, Set<String> oldFacetIds, boolean dirty) {
        for (FacetEditRow row : selectedFacets) {
            DirtyUpdate next = addSelectedFacet(ctx, row, oldFacetIds, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate addSelectedFacet(LabelWriteContext ctx, FacetEditRow row, Set<String> oldFacetIds, boolean dirty) {
        if (row == null || StringUtils.isBlank(row.getId()) || oldFacetIds.contains(row.getId())) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult added = facetMutationService.addMember(
                new AddFacetMemberCommand(ctx.thesaurusId(), row.getId(), ctx.conceptId(), false));
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
        return Strings.CS.equals(editingConceptId, detail.getSummary().getConceptId())
                && Strings.CS.equals(editingLang, resolveLang(detail));
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

    static Optional<List<FacetEditRow>> parseFacetsJson(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "[]".equals(trimmed)) {
            return Optional.of(new ArrayList<>());
        }
        try {
            JsonNode root = FACET_JSON.readTree(trimmed);
            if (!root.isArray()) {
                return Optional.empty();
            }
            List<FacetEditRow> rows = new ArrayList<>();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (JsonNode node : root) {
                addParsedFacet(rows, seen, node);
            }
            return Optional.of(rows);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static void addParsedFacet(List<FacetEditRow> rows, Set<String> seen, JsonNode node) {
        if (node == null || !node.isObject()) {
            return;
        }
        String id = node.path("id").asText("");
        if (StringUtils.isBlank(id) || !seen.add(id)) {
            return;
        }
        rows.add(new FacetEditRow(id, node.path("label").asText("")));
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

    private record LabelWriteContext(
            String thesaurusId,
            String conceptId,
            String lang,
            int userId,
            String contributor,
            boolean forced
    ) {
    }

    private record SynonymSets(
            List<String> newAlts,
            List<String> newHidden,
            Set<String> oldAltSet,
            Set<String> oldHiddenSet,
            Set<String> newAltSet,
            Set<String> newHiddenSet
    ) {
        static SynonymSets from(
                List<String> oldAlts, List<String> oldHidden, List<String> newAlts, List<String> newHidden) {
            return new SynonymSets(
                    newAlts,
                    newHidden,
                    new LinkedHashSet<>(oldAlts),
                    new LinkedHashSet<>(oldHidden),
                    new LinkedHashSet<>(newAlts),
                    new LinkedHashSet<>(newHidden));
        }

        Set<String> oldAll() {
            return union(oldAltSet, oldHiddenSet);
        }

        Set<String> newAll() {
            return union(newAltSet, newHiddenSet);
        }
    }
}
