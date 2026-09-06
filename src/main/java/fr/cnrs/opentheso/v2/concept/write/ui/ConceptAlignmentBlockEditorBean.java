package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignmentGroup;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
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

/**
 * Édition inline du bloc Alignement (maquette : URI + type + source).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptAlignmentBlockEditorBean")
@RequiredArgsConstructor
public class ConceptAlignmentBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "alignementEdit";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptAlignmentMutationService conceptAlignmentMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private List<AlignmentBlockEditRow> rows = new ArrayList<>();
    private List<ConceptWriteAlignmentType> alignmentTypes = new ArrayList<>();
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedConcept() != null
                && conceptWritePolicy.canMutateAlignments(
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
        alignmentTypes = conceptAlignmentMutationService.listAlignmentTypes();
        rows = copyRows(detail);
        if (rows.isEmpty()) {
            rows.add(newRow());
        }
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
        rows.add(newRow());
    }

    public void setRowType(int index, int typeId) {
        if (!isEditing() || index < 0 || index >= rows.size() || typeId <= 0) {
            return;
        }
        rows.get(index).setTypeId(typeId);
    }

    public void removeRow(int index) {
        if (!isEditing() || index < 0 || index >= rows.size()) {
            return;
        }
        rows.remove(index);
        if (rows.isEmpty()) {
            rows.add(newRow());
        }
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

        List<AlignmentBlockEditRow> kept = new ArrayList<>();
        Set<String> seenUris = new LinkedHashSet<>();
        for (AlignmentBlockEditRow row : rows) {
            if (row == null) {
                continue;
            }
            String uri = StringUtils.trimToEmpty(row.getUri());
            if (uri.isEmpty()) {
                if (row.isExisting()) {
                    errorMessage = "L'URI est obligatoire !";
                    return;
                }
                continue;
            }
            if (row.getTypeId() <= 0) {
                errorMessage = "Le type d'alignement est obligatoire !";
                return;
            }
            String uriKey = uri.toLowerCase();
            if (!seenUris.add(uriKey)) {
                errorMessage = "Chaque URI ne peut apparaître qu'une fois.";
                return;
            }
            kept.add(row);
        }

        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        Set<Integer> keptIds = new LinkedHashSet<>();
        for (AlignmentBlockEditRow row : kept) {
            if (row.isExisting() && row.getAlignmentId() > 0) {
                keptIds.add(row.getAlignmentId());
            }
        }

        boolean dirty = false;
        for (ConceptAlignment alignment : flatten(current)) {
            int id = parseAlignmentId(alignment.id());
            if (id <= 0 || keptIds.contains(id)) {
                continue;
            }
            MutationResult deleted = conceptAlignmentMutationService.deleteAlignment(
                    new DeleteAlignmentCommand(thesaurusId, conceptId, id, userId, contributor));
            if (!applyResult(deleted, dirty)) {
                return;
            }
            dirty = true;
        }

        for (AlignmentBlockEditRow row : kept) {
            if (row.isExisting() && row.getAlignmentId() > 0) {
                MutationResult updated = conceptAlignmentMutationService.updateAlignment(
                        new UpdateAlignmentCommand(
                                thesaurusId,
                                conceptId,
                                row.getAlignmentId(),
                                row.getTypeId(),
                                row.getUri(),
                                StringUtils.trimToEmpty(row.getSource()),
                                userId,
                                contributor
                        ));
                if (!applyResult(updated, dirty)) {
                    return;
                }
                dirty = true;
            } else {
                MutationResult added = conceptAlignmentMutationService.addManualAlignment(
                        new AddManualAlignmentCommand(
                                thesaurusId,
                                conceptId,
                                row.getTypeId(),
                                row.getUri(),
                                StringUtils.trimToEmpty(row.getSource()),
                                userId,
                                contributor
                        ));
                if (!applyResult(added, dirty)) {
                    return;
                }
                dirty = true;
            }
        }

        finishSuccess();
    }

    private AlignmentBlockEditRow newRow() {
        return new AlignmentBlockEditRow(0, defaultTypeId(), "", "", false);
    }

    private int defaultTypeId() {
        return alignmentTypes.stream()
                .filter(type -> type.getId() == 1)
                .map(ConceptWriteAlignmentType::getId)
                .findFirst()
                .orElse(alignmentTypes.isEmpty() ? 1 : alignmentTypes.get(0).getId());
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
        flashMessage = "Alignements enregistrés";
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
        rows = new ArrayList<>();
        alignmentTypes = new ArrayList<>();
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

    private static List<AlignmentBlockEditRow> copyRows(ConceptDetail detail) {
        List<AlignmentBlockEditRow> copied = new ArrayList<>();
        for (ConceptAlignment alignment : flatten(detail)) {
            copied.add(new AlignmentBlockEditRow(
                    parseAlignmentId(alignment.id()),
                    alignment.typeId(),
                    StringUtils.defaultString(alignment.uri()),
                    StringUtils.defaultString(alignment.sourceName()),
                    true
            ));
        }
        return copied;
    }

    private static List<ConceptAlignment> flatten(ConceptDetail detail) {
        List<ConceptAlignment> alignments = new ArrayList<>();
        if (detail == null || detail.getAlignmentGroups() == null) {
            return alignments;
        }
        for (ConceptAlignmentGroup group : detail.getAlignmentGroups()) {
            if (group == null || group.items() == null) {
                continue;
            }
            alignments.addAll(group.items());
        }
        return alignments;
    }

    private static int parseAlignmentId(String rawId) {
        if (StringUtils.isBlank(rawId) || !StringUtils.isNumeric(rawId.trim())) {
            return 0;
        }
        return Integer.parseInt(rawId.trim());
    }
}
