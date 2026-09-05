package fr.cnrs.opentheso.v2.concept.write.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
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
import java.util.stream.Collectors;

/**
 * Édition inline du bloc Relation personnalisée sans dialogue.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptCustomRelationBlockEditorBean")
@RequiredArgsConstructor
public class ConceptCustomRelationBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "relPerso";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptRelationMutationService conceptRelationMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private List<CustomRelationEditRow> selectedRelations = new ArrayList<>();
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedConcept() != null
                && thesaurusViewBean.isCustomRelationVisible()
                && conceptWritePolicy.canMutateCustomRelations(
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
        selectedRelations = copyRelations(detail.getCustomRelations());
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

    public String getSelectedRelationsJson() {
        return toJson(selectedRelations);
    }

    public void setSelectedRelationsJson(String json) {
        List<CustomRelationEditRow> parsed = parseJson(json);
        if (parsed != null) {
            selectedRelations = parsed;
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

        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String contributor = StringUtils.defaultString(userSession.getCurrentUsername());
        String selfId = normalizeId(conceptId);
        Set<String> newIds = selectedIds();
        if (newIds.contains(selfId)) {
            errorMessage = "Relation non permise !";
            return;
        }

        boolean dirty = false;
        List<ConceptCustomRelationItem> currentRelations = current.getCustomRelations() == null
                ? List.of()
                : current.getCustomRelations();

        for (ConceptCustomRelationItem relation : currentRelations) {
            if (relation == null || StringUtils.isBlank(relation.getTargetConceptId())) {
                continue;
            }
            if (newIds.contains(normalizeId(relation.getTargetConceptId()))) {
                continue;
            }
            MutationResult removed = conceptRelationMutationService.deleteCustomRelation(
                    new DeleteCustomRelationCommand(
                            thesaurusId,
                            conceptId,
                            relation.getTargetConceptId(),
                            relation.getRelationCode(),
                            relation.isReciprocal(),
                            userId,
                            contributor));
            if (!applyResult(removed, dirty)) {
                return;
            }
            dirty = true;
        }

        Set<String> oldIds = normalizedIds(currentRelations);
        for (CustomRelationEditRow row : selectedRelations) {
            if (row == null || StringUtils.isBlank(row.getId())) {
                continue;
            }
            if (oldIds.contains(normalizeId(row.getId()))) {
                continue;
            }
            MutationResult added = conceptRelationMutationService.addCustomRelation(
                    new AddCustomRelationCommand(
                            thesaurusId, conceptId, row.getId(), userId, contributor));
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
        flashMessage = "Relations personnalisées enregistrées";
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
        selectedRelations = new ArrayList<>();
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

    private Set<String> selectedIds() {
        return selectedRelations.stream()
                .map(CustomRelationEditRow::getId)
                .map(ConceptCustomRelationBlockEditorBean::normalizeId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> normalizedIds(List<ConceptCustomRelationItem> relations) {
        if (relations == null || relations.isEmpty()) {
            return Set.of();
        }
        return relations.stream()
                .map(ConceptCustomRelationItem::getTargetConceptId)
                .map(ConceptCustomRelationBlockEditorBean::normalizeId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<CustomRelationEditRow> copyRelations(List<ConceptCustomRelationItem> relations) {
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>();
        }
        List<CustomRelationEditRow> rows = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (ConceptCustomRelationItem relation : relations) {
            if (relation == null || StringUtils.isBlank(relation.getTargetConceptId())) {
                continue;
            }
            if (!seen.add(normalizeId(relation.getTargetConceptId()))) {
                continue;
            }
            rows.add(new CustomRelationEditRow(
                    relation.getTargetConceptId(),
                    relation.getTargetLabel(),
                    relation.getRelationCode(),
                    relation.getRelationLabel(),
                    relation.isReciprocal()));
        }
        return rows;
    }

    static String toJson(List<CustomRelationEditRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (CustomRelationEditRow row : rows) {
            if (row == null || StringUtils.isBlank(row.getId())) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("{\"id\":").append(jsonQuote(row.getId()))
                    .append(",\"label\":").append(jsonQuote(row.getDisplayLabel()))
                    .append(",\"role\":").append(jsonQuote(row.getRole()))
                    .append(",\"roleLabel\":").append(jsonQuote(row.getDisplayRole()))
                    .append(",\"reciprocal\":").append(row.isReciprocal())
                    .append('}');
        }
        return sb.append(']').toString();
    }

    static List<CustomRelationEditRow> parseJson(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "[]".equals(trimmed)) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = JSON.readTree(trimmed);
            if (!root.isArray()) {
                return null;
            }
            List<CustomRelationEditRow> rows = new ArrayList<>();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (JsonNode node : root) {
                if (node == null || !node.isObject()) {
                    continue;
                }
                String id = node.path("id").asText("");
                if (StringUtils.isBlank(id) || !seen.add(normalizeId(id))) {
                    continue;
                }
                rows.add(new CustomRelationEditRow(
                        id,
                        node.path("label").asText(""),
                        node.path("role").asText(""),
                        node.path("roleLabel").asText(""),
                        node.path("reciprocal").asBoolean(false)));
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

    private static String normalizeId(String id) {
        return StringUtils.isBlank(id) ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
