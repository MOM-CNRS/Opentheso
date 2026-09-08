package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptRelation;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptToCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RemoveConceptFromCollectionCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptCollectionMutationService;
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
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Édition inline du bloc Collections sans dialogue.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptCollectionBlockEditorBean")
@RequiredArgsConstructor
public class ConceptCollectionBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "collections";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptCollectionMutationService conceptCollectionMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private List<FacetEditRow> selectedCollections = new ArrayList<>();
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedConcept() != null
                && conceptWritePolicy.canMutateConceptAttributes(
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
        selectedCollections = copyCollections(detail.getCollections());
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

    public String getSelectedCollectionsJson() {
        return ConceptLabelBlockEditorBean.toFacetsJson(selectedCollections);
    }

    public void setSelectedCollectionsJson(String json) {
        ConceptLabelBlockEditorBean.parseFacetsJson(json).ifPresent(parsed -> selectedCollections = parsed);
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

        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());

        if (!syncCollections(current, thesaurusId, conceptId, userId, contributor)) {
            return;
        }
        finishSuccess();
    }

    private boolean syncCollections(
            ConceptDetail current,
            String thesaurusId,
            String conceptId,
            int userId,
            String contributor
    ) {
        List<ConceptRelation> currentCollections = current.getCollections() == null
                ? List.of()
                : current.getCollections();
        Set<String> oldIds = normalizedIds(currentCollections);
        Set<String> newIds = selectedCollectionIds();
        CollectionWriteContext ctx = new CollectionWriteContext(thesaurusId, conceptId, userId, contributor);
        DirtyUpdate dirty = removeUnselectedCollections(currentCollections, newIds, ctx, false);
        if (!dirty.ok) {
            return false;
        }
        return addSelectedCollections(oldIds, ctx, dirty.dirty).ok;
    }

    private DirtyUpdate removeUnselectedCollections(
            List<ConceptRelation> currentCollections,
            Set<String> newIds,
            CollectionWriteContext ctx,
            boolean dirty
    ) {
        for (ConceptRelation collection : currentCollections) {
            DirtyUpdate next = removeUnselectedCollection(collection, newIds, ctx, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate removeUnselectedCollection(
            ConceptRelation collection, Set<String> newIds, CollectionWriteContext ctx, boolean dirty) {
        if (collection == null || StringUtils.isBlank(collection.getConceptId())) {
            return DirtyUpdate.of(dirty);
        }
        if (newIds.contains(normalizeId(collection.getConceptId()))) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult removed = conceptCollectionMutationService.removeFromCollection(
                new RemoveConceptFromCollectionCommand(
                        ctx.thesaurusId(), ctx.conceptId(), ctx.userId(), ctx.contributor(),
                        collection.getConceptId(), false));
        if (!applyResult(removed, dirty)) {
            return DirtyUpdate.fail();
        }
        return DirtyUpdate.of(true);
    }

    private DirtyUpdate addSelectedCollections(Set<String> oldIds, CollectionWriteContext ctx, boolean dirty) {
        for (FacetEditRow row : selectedCollections) {
            DirtyUpdate next = addSelectedCollection(row, oldIds, ctx, dirty);
            if (!next.ok) {
                return DirtyUpdate.fail();
            }
            dirty = next.dirty;
        }
        return DirtyUpdate.of(dirty);
    }

    private DirtyUpdate addSelectedCollection(
            FacetEditRow row, Set<String> oldIds, CollectionWriteContext ctx, boolean dirty) {
        if (row == null || StringUtils.isBlank(row.getId())) {
            return DirtyUpdate.of(dirty);
        }
        if (oldIds.contains(normalizeId(row.getId()))) {
            return DirtyUpdate.of(dirty);
        }
        MutationResult added = conceptCollectionMutationService.addToCollection(
                new AddConceptToCollectionCommand(
                        ctx.thesaurusId(), ctx.conceptId(), ctx.userId(), ctx.contributor(), row.getId(), false));
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
        flashMessage = "Collections enregistrées";
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
        selectedCollections = new ArrayList<>();
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

    private Set<String> selectedCollectionIds() {
        return selectedCollections.stream()
                .map(FacetEditRow::getId)
                .map(ConceptCollectionBlockEditorBean::normalizeId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> normalizedIds(List<ConceptRelation> collections) {
        if (collections == null || collections.isEmpty()) {
            return Set.of();
        }
        return collections.stream()
                .map(ConceptRelation::getConceptId)
                .map(ConceptCollectionBlockEditorBean::normalizeId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<FacetEditRow> copyCollections(List<ConceptRelation> collections) {
        if (collections == null || collections.isEmpty()) {
            return new ArrayList<>();
        }
        List<FacetEditRow> rows = new ArrayList<>();
        for (ConceptRelation collection : collections) {
            if (collection == null || StringUtils.isBlank(collection.getConceptId())) {
                continue;
            }
            rows.add(new FacetEditRow(collection.getConceptId(), collection.getDisplayLabel()));
        }
        return rows;
    }

    private static String normalizeId(String id) {
        return StringUtils.isBlank(id) ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private record CollectionWriteContext(String thesaurusId, String conceptId, int userId, String contributor) {
    }
}
