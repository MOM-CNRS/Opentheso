package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptRelationMutationService;
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
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Édition inline du bloc Relations sémantiques (TG / TS / TA) sans dialogue.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptRelationBlockEditorBean")
@RequiredArgsConstructor
public class ConceptRelationBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "relations";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptRelationMutationService conceptRelationMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private String editingLang;
    private List<FacetEditRow> selectedBroader = new ArrayList<>();
    private List<FacetEditRow> selectedNarrower = new ArrayList<>();
    private List<FacetEditRow> selectedRelated = new ArrayList<>();
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
        selectedNarrower = copyRelations(detail.getNarrowerTerms());
        selectedRelated = copyRelations(detail.getRelatedTerms());
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
        List<FacetEditRow> parsed = ConceptLabelBlockEditorBean.parseFacetsJson(json);
        if (parsed != null) {
            selectedBroader = parsed;
        }
    }

    public String getSelectedNarrowerJson() {
        return ConceptLabelBlockEditorBean.toFacetsJson(selectedNarrower);
    }

    public void setSelectedNarrowerJson(String json) {
        List<FacetEditRow> parsed = ConceptLabelBlockEditorBean.parseFacetsJson(json);
        if (parsed != null) {
            selectedNarrower = parsed;
        }
    }

    public String getSelectedRelatedJson() {
        return ConceptLabelBlockEditorBean.toFacetsJson(selectedRelated);
    }

    public void setSelectedRelatedJson(String json) {
        List<FacetEditRow> parsed = ConceptLabelBlockEditorBean.parseFacetsJson(json);
        if (parsed != null) {
            selectedRelated = parsed;
        }
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
        hierarchicalDirty = hierarchicalDirty || deleted.applied();
        if (!deleted.ok()) {
            treeReload = hierarchicalDirty;
            return;
        }

        deleted = applyDeletes(
                current.getNarrowerTerms(),
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
                current.getNarrowerTerms(),
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

        treeReload = hierarchicalDirty;
        finishSuccess();
    }

    private MutationPass applyDeletes(
            List<ConceptRelation> current,
            Set<String> newIds,
            Function<String, MutationResult> deleter,
            boolean dirty
    ) {
        boolean applied = false;
        List<ConceptRelation> relations = current == null ? List.of() : current;
        for (ConceptRelation relation : relations) {
            if (relation == null || StringUtils.isBlank(relation.getConceptId())) {
                continue;
            }
            if (newIds.contains(normalizeId(relation.getConceptId()))) {
                continue;
            }
            MutationResult removed = deleter.apply(relation.getConceptId());
            if (!applyResult(removed, dirty)) {
                return new MutationPass(dirty, applied, false);
            }
            dirty = true;
            applied = true;
        }
        return new MutationPass(dirty, applied, true);
    }

    private MutationPass applyAdds(
            List<ConceptRelation> current,
            List<FacetEditRow> selected,
            Function<String, MutationResult> adder,
            boolean dirty
    ) {
        boolean applied = false;
        Set<String> oldIds = normalizedIds(current);
        for (FacetEditRow row : selected) {
            if (row == null || StringUtils.isBlank(row.getId())) {
                continue;
            }
            if (oldIds.contains(normalizeId(row.getId()))) {
                continue;
            }
            MutationResult added = adder.apply(row.getId());
            if (!applyResult(added, dirty)) {
                return new MutationPass(dirty, applied, false);
            }
            dirty = true;
            applied = true;
        }
        return new MutationPass(dirty, applied, true);
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
            errorMessage = "Relation non permise !";
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
        return StringUtils.equals(editingConceptId, detail.getSummary().getConceptId());
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

    private static List<FacetEditRow> copyRelations(List<ConceptRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<FacetEditRow> rows = new ArrayList<>();
        for (ConceptRelation relation : relations) {
            if (relation == null || StringUtils.isBlank(relation.getConceptId())) {
                continue;
            }
            rows.add(new FacetEditRow(relation.getConceptId(), relation.getDisplayLabel()));
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

    private static String normalizeId(String id) {
        return StringUtils.isBlank(id) ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private record MutationPass(boolean dirty, boolean applied, boolean ok) {
    }
}
