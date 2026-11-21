package fr.cnrs.opentheso.concept.write.ui;

import fr.cnrs.opentheso.concept.model.ConceptDetail;
import fr.cnrs.opentheso.concept.model.ConceptHierarchicalRelation;
import fr.cnrs.opentheso.concept.model.ConceptRelation;
import fr.cnrs.opentheso.concept.service.ConceptFullReadService;
import fr.cnrs.opentheso.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.concept.write.model.MutationResult;
import fr.cnrs.opentheso.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.concept.write.service.ConceptRelationMutationService;
import fr.cnrs.opentheso.shared.ui.UserSession;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Édition inline du bloc Relations sémantiques (TG / TS / TA) sans dialogue.
 */
@Getter
@Setter
@ViewScoped
@Named("conceptRelationBlockEditorBean")
@RequiredArgsConstructor
public class ConceptRelationBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "relations";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptRelationMutationService conceptRelationMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptFullReadService conceptFullReadService;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private String editingLang;
    private List<FacetEditRow> selectedBroader = new ArrayList<>();
    private List<FacetEditRow> selectedNarrower = new ArrayList<>();
    private List<FacetEditRow> selectedRelated = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private List<ConceptRelation> baselineNarrower = new ArrayList<>();
    private Map<String, String> baselineBroaderRoles = new LinkedHashMap<>();
    private Map<String, String> baselineNarrowerRoles = new LinkedHashMap<>();
    private List<FacetEditRow> snapshotBroader = new ArrayList<>();
    private List<FacetEditRow> snapshotNarrower = new ArrayList<>();
    private List<FacetEditRow> snapshotRelated = new ArrayList<>();
    private String editingKind;
    private String errorMessage;
    private String flashMessage;
    private String flashToken;
    private boolean treeReload;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedConcept() != null
                && conceptWritePolicy.canMutateHierarchicalRelations(
                        userSession, thesaurusViewBean.isSelectedConceptDeprecated());
    }

    public boolean isEditing() {
        if (editing && !matchesCurrentConcept()) {
            resetForm(false);
        }
        return editing && FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard());
    }

    public void startEditing() {
        openEditor(null);
    }

    public void startEditing(String kind) {
        String normalized = normalizeKind(kind);
        if (normalized == null) {
            return;
        }
        openEditor(normalized);
    }

    public boolean isEditingBroader() {
        return isEditing() && "bt".equals(editingKind);
    }

    public boolean isEditingNarrower() {
        return isEditing() && "nt".equals(editingKind);
    }

    public boolean isEditingRelated() {
        return isEditing() && "rt".equals(editingKind);
    }

    private void openEditor(String kind) {
        if (!isEditable()) {
            return;
        }
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return;
        }
        editingConceptId = detail.getSummary().getConceptId();
        editingLang = resolveLang(detail);
        selectedBroader = copyRelations(detail.getBroaderTerms());
        selectedNarrower = copyRelations(loadAllNarrowerRelations(detail));
        selectedRelated = copyRelations(detail.getRelatedTerms());
        baselineBroaderRoles = roleIndex(detail.getBroaderTerms());
        baselineNarrowerRoles = roleIndex(baselineNarrower);
        snapshotBroader = copyRows(selectedBroader);
        snapshotNarrower = copyRows(selectedNarrower);
        snapshotRelated = copyRows(selectedRelated);
        editingKind = kind;
        errorMessage = "";
        flashMessage = "";
        flashToken = "";
        treeReload = false;
        editing = true;
        thesaurusViewBean.setFicheEditCard(FICHE_CARD);
        conceptSelectionContext.update(thesaurusViewBean.getId(), detail);
    }

    public void cancel() {
        resetForm(false);
    }

    public String getSelectedBroaderJson() {
        return ConceptLabelBlockEditorBean.toFacetsJson(selectedBroader);
    }

    public void setSelectedBroaderJson(String json) {
        ConceptLabelBlockEditorBean.parseFacetsJson(json).ifPresent(parsed -> selectedBroader = parsed);
    }

    public String getSelectedNarrowerJson() {
        return ConceptLabelBlockEditorBean.toFacetsJson(selectedNarrower);
    }

    public void setSelectedNarrowerJson(String json) {
        ConceptLabelBlockEditorBean.parseFacetsJson(json).ifPresent(parsed -> selectedNarrower = parsed);
    }

    public String getSelectedRelatedJson() {
        return ConceptLabelBlockEditorBean.toFacetsJson(selectedRelated);
    }

    public void setSelectedRelatedJson(String json) {
        ConceptLabelBlockEditorBean.parseFacetsJson(json).ifPresent(parsed -> selectedRelated = parsed);
    }

    public void save() {
        errorMessage = "";
        treeReload = false;
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

        restoreUntouchedKinds();

        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        String lang = StringUtils.firstNonBlank(editingLang, resolveLang(current), "fr");

        Set<String> broaderIds = selectedIds(selectedBroader);
        Set<String> narrowerIds = selectedIds(selectedNarrower);
        Set<String> relatedIds = selectedIds(selectedRelated);
        if (!validateRelationSets(conceptId, broaderIds, narrowerIds, relatedIds)) {
            return;
        }

        boolean dirty = false;
        boolean hierarchicalDirty = false;

        MutationPass deleted = applyDeletes(
                current.getBroaderTerms(),
                broaderIds,
                targetId -> conceptRelationMutationService.deleteBroaderRelation(
                        new DeleteBroaderRelationCommand(
                                thesaurusId, conceptId, targetId, userId, contributor)),
                dirty);
        dirty = deleted.dirty();
        hierarchicalDirty = deleted.applied();
        if (!deleted.ok()) {
            treeReload = hierarchicalDirty;
            return;
        }

        deleted = applyDeletes(
                baselineNarrower,
                narrowerIds,
                targetId -> conceptRelationMutationService.deleteNarrowerRelation(
                        new DeleteNarrowerRelationCommand(
                                thesaurusId, conceptId, targetId, userId, contributor)),
                dirty);
        dirty = deleted.dirty();
        hierarchicalDirty = hierarchicalDirty || deleted.applied();
        if (!deleted.ok()) {
            treeReload = hierarchicalDirty;
            return;
        }

        deleted = applyDeletes(
                current.getRelatedTerms(),
                relatedIds,
                targetId -> conceptRelationMutationService.deleteRelatedRelation(
                        new DeleteRelatedRelationCommand(
                                thesaurusId, conceptId, targetId, userId, contributor)),
                dirty);
        dirty = deleted.dirty();
        if (!deleted.ok()) {
            treeReload = hierarchicalDirty;
            return;
        }

        MutationPass added = applyAdds(
                current.getBroaderTerms(),
                selectedBroader,
                targetId -> conceptRelationMutationService.addBroaderRelation(
                        new AddBroaderRelationCommand(
                                thesaurusId, conceptId, targetId, userId, contributor)),
                dirty);
        dirty = added.dirty();
        hierarchicalDirty = hierarchicalDirty || added.applied();
        if (!added.ok()) {
            treeReload = hierarchicalDirty;
            return;
        }

        added = applyAdds(
                baselineNarrower,
                selectedNarrower,
                targetId -> conceptRelationMutationService.addNarrowerRelation(
                        new AddNarrowerRelationCommand(
                                thesaurusId, conceptId, targetId, userId, contributor)),
                dirty);
        dirty = added.dirty();
        hierarchicalDirty = hierarchicalDirty || added.applied();
        if (!added.ok()) {
            treeReload = hierarchicalDirty;
            return;
        }

        added = applyAdds(
                current.getRelatedTerms(),
                selectedRelated,
                targetId -> conceptRelationMutationService.addRelatedRelation(
                        new AddRelatedRelationCommand(
                                thesaurusId, conceptId, targetId, lang, userId, contributor, false)),
                dirty);
        if (!added.ok()) {
            treeReload = hierarchicalDirty;
            return;
        }

        if (!applyRoleUpdates(
                true, selectedBroader, baselineBroaderRoles, thesaurusId, conceptId, userId, contributor)) {
            treeReload = true;
            return;
        }
        if (!applyRoleUpdates(
                false, selectedNarrower, baselineNarrowerRoles, thesaurusId, conceptId, userId, contributor)) {
            treeReload = true;
            return;
        }

        treeReload = hierarchicalDirty;
        finishSuccess();
    }

    private MutationPass applyDeletes(
            List<ConceptRelation> current,
            Set<String> newIds,
            Function<String, MutationResult> deleter,
            boolean dirty
    ) {
        MutationPass pass = new MutationPass(dirty, false, true);
        List<ConceptRelation> relations = current == null ? List.of() : current;
        for (ConceptRelation relation : relations) {
            MutationPass next = deleteUnselectedRelation(relation, newIds, deleter, pass);
            if (!next.ok()) {
                return next;
            }
            pass = next;
        }
        return pass;
    }

    private MutationPass deleteUnselectedRelation(
            ConceptRelation relation,
            Set<String> newIds,
            Function<String, MutationResult> deleter,
            MutationPass pass
    ) {
        if (relation == null || StringUtils.isBlank(relation.getConceptId())) {
            return pass;
        }
        if (newIds.contains(normalizeId(relation.getConceptId()))) {
            return pass;
        }
        MutationResult removed = deleter.apply(relation.getConceptId());
        if (!applyResult(removed, pass.dirty())) {
            return new MutationPass(pass.dirty(), pass.applied(), false);
        }
        return new MutationPass(true, true, true);
    }

    private MutationPass applyAdds(
            List<ConceptRelation> current,
            List<FacetEditRow> selected,
            Function<String, MutationResult> adder,
            boolean dirty
    ) {
        MutationPass pass = new MutationPass(dirty, false, true);
        Set<String> oldIds = normalizedIds(current);
        for (FacetEditRow row : selected) {
            MutationPass next = addSelectedRelation(row, oldIds, adder, pass);
            if (!next.ok()) {
                return next;
            }
            pass = next;
        }
        return pass;
    }

    private MutationPass addSelectedRelation(
            FacetEditRow row,
            Set<String> oldIds,
            Function<String, MutationResult> adder,
            MutationPass pass
    ) {
        if (row == null || StringUtils.isBlank(row.getId())) {
            return pass;
        }
        if (oldIds.contains(normalizeId(row.getId()))) {
            return pass;
        }
        MutationResult added = adder.apply(row.getId());
        if (!applyResult(added, pass.dirty())) {
            return new MutationPass(pass.dirty(), pass.applied(), false);
        }
        return new MutationPass(true, true, true);
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
        flashMessage = "Relations enregistrées";
        flashToken = String.valueOf(System.currentTimeMillis());
        thesaurusViewBean.reloadSelectedConcept();
        conceptSelectionContext.update(thesaurusViewBean.getId(), thesaurusViewBean.getSelectedConcept());
    }

    private boolean validateRelationSets(
            String conceptId,
            Set<String> broaderIds,
            Set<String> narrowerIds,
            Set<String> relatedIds
    ) {
        if (hasOverlap(broaderIds, narrowerIds) || hasOverlap(broaderIds, relatedIds)
                || hasOverlap(narrowerIds, relatedIds)) {
            errorMessage = "Un concept ne peut pas avoir plusieurs types de relation à la fois.";
            return false;
        }
        String selfId = normalizeId(conceptId);
        if (broaderIds.contains(selfId) || narrowerIds.contains(selfId) || relatedIds.contains(selfId)) {
            errorMessage = "Un concept ne peut pas être en relation avec lui-même.";
            return false;
        }
        return true;
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
        selectedBroader = new ArrayList<>();
        selectedNarrower = new ArrayList<>();
        selectedRelated = new ArrayList<>();
        baselineNarrower = new ArrayList<>();
        baselineBroaderRoles = new LinkedHashMap<>();
        baselineNarrowerRoles = new LinkedHashMap<>();
        snapshotBroader = new ArrayList<>();
        snapshotNarrower = new ArrayList<>();
        snapshotRelated = new ArrayList<>();
        editingKind = null;
        errorMessage = "";
        treeReload = false;
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

    private String resolveLang(ConceptDetail detail) {
        if (detail != null && detail.getSummary() != null && StringUtils.isNotBlank(detail.getSummary().getLang())) {
            return detail.getSummary().getLang();
        }
        return StringUtils.defaultIfBlank(thesaurusViewBean.getSelectedLang(), "fr");
    }

    private static Set<String> selectedIds(List<FacetEditRow> rows) {
        return rows.stream()
                .map(FacetEditRow::getId)
                .map(ConceptRelationBlockEditorBean::normalizeId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> normalizedIds(List<ConceptRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return Set.of();
        }
        return relations.stream()
                .map(ConceptRelation::getConceptId)
                .map(ConceptRelationBlockEditorBean::normalizeId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<ConceptRelation> loadAllNarrowerRelations(ConceptDetail detail) {
        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = detail.getSummary() == null ? "" : detail.getSummary().getConceptId();
        String lang = StringUtils.firstNonBlank(editingLang, resolveLang(detail), "fr");
        List<ConceptHierarchicalRelation> all = conceptFullReadService.loadAllNarrowers(
                thesaurusId, conceptId, lang, userSession.isLoggedIn());
        if (all == null || all.isEmpty()) {
            baselineNarrower = copyRelationModels(detail.getNarrowerTerms());
            return baselineNarrower;
        }
        baselineNarrower = new ArrayList<>();
        for (ConceptHierarchicalRelation relation : all) {
            if (relation == null || StringUtils.isBlank(relation.conceptId())) {
                continue;
            }
            baselineNarrower.add(new ConceptRelation(
                    relation.conceptId(),
                    StringUtils.defaultString(relation.label()),
                    "",
                    StringUtils.defaultString(relation.role())));
        }
        return baselineNarrower;
    }

    private static List<ConceptRelation> copyRelationModels(List<ConceptRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(relations);
    }

    private void restoreUntouchedKinds() {
        if ("bt".equals(editingKind)) {
            selectedNarrower = copyRows(snapshotNarrower);
            selectedRelated = copyRows(snapshotRelated);
        } else if ("nt".equals(editingKind)) {
            selectedBroader = copyRows(snapshotBroader);
            selectedRelated = copyRows(snapshotRelated);
        } else if ("rt".equals(editingKind)) {
            selectedBroader = copyRows(snapshotBroader);
            selectedNarrower = copyRows(snapshotNarrower);
        }
    }

    private static List<FacetEditRow> copyRows(List<FacetEditRow> rows) {
        List<FacetEditRow> copy = new ArrayList<>();
        if (rows == null) {
            return copy;
        }
        for (FacetEditRow row : rows) {
            if (row == null || StringUtils.isBlank(row.getId())) {
                continue;
            }
            copy.add(new FacetEditRow(row.getId(), row.getLabel(), row.getRole()));
        }
        return copy;
    }

    private static String normalizeKind(String kind) {
        return switch (StringUtils.trimToEmpty(kind).toLowerCase(Locale.ROOT)) {
            case "bt", "nt", "rt" -> kind.trim().toLowerCase(Locale.ROOT);
            default -> null;
        };
    }

    private static List<FacetEditRow> copyRelations(List<ConceptRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<FacetEditRow> rows = new ArrayList<>();
        for (ConceptRelation relation : relations) {
            if (relation == null || StringUtils.isBlank(relation.getConceptId())) {
                continue;
            }
            rows.add(new FacetEditRow(
                    relation.getConceptId(), relation.getDisplayLabel(), relation.getRole()));
        }
        return rows;
    }

    private static boolean hasOverlap(Set<String> left, Set<String> right) {
        for (String id : left) {
            if (right.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean applyRoleUpdates(
            boolean broader,
            List<FacetEditRow> selected,
            Map<String, String> baseline,
            String thesaurusId,
            String conceptId,
            int userId,
            String contributor
    ) {
        Map<String, String> previousRoles = baseline == null ? Map.of() : baseline;
        String fallback = broader ? "BT" : "NT";
        for (FacetEditRow row : selected) {
            if (row == null || StringUtils.isBlank(row.getId())) {
                continue;
            }
            String previous = StringUtils.trimToEmpty(previousRoles.get(normalizeId(row.getId())));
            String wanted = directHierarchicalRole(broader, row.getRole());
            String current = previous.isEmpty() ? fallback : previous;
            if (wanted.equalsIgnoreCase(current)) {
                continue;
            }
            MutationResult updated = conceptRelationMutationService.updateHierarchicalRole(
                    thesaurusId, conceptId, row.getId(), wanted, userId, contributor);
            if (!applyResult(updated, true)) {
                return false;
            }
        }
        return true;
    }

    static String directHierarchicalRole(boolean broader, String requested) {
        return switch (hierarchicalFamily(requested)) {
            case "G" -> broader ? "BTG" : "NTG";
            case "P" -> broader ? "BTP" : "NTP";
            case "I" -> broader ? "BTI" : "NTI";
            default -> broader ? "BT" : "NT";
        };
    }

    private static String hierarchicalFamily(String role) {
        String code = StringUtils.trimToEmpty(role).toUpperCase(Locale.ROOT);
        if ("BTG".equals(code) || "NTG".equals(code) || "G".equals(code)) {
            return "G";
        }
        if ("BTP".equals(code) || "NTP".equals(code) || "P".equals(code)) {
            return "P";
        }
        if ("BTI".equals(code) || "NTI".equals(code) || "I".equals(code)) {
            return "I";
        }
        return "S";
    }

    private static Map<String, String> roleIndex(List<ConceptRelation> relations) {
        Map<String, String> roles = new LinkedHashMap<>();
        if (relations == null) {
            return roles;
        }
        for (ConceptRelation relation : relations) {
            if (relation == null || StringUtils.isBlank(relation.getConceptId())) {
                continue;
            }
            roles.put(normalizeId(relation.getConceptId()), StringUtils.trimToEmpty(relation.getRole()));
        }
        return roles;
    }

    private static String normalizeId(String id) {
        return StringUtils.isBlank(id) ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private record MutationPass(boolean dirty, boolean applied, boolean ok) {
    }
}
