package fr.cnrs.opentheso.v2.facet.ui;

import fr.cnrs.opentheso.v2.concept.model.FacetDetailOverview;
import fr.cnrs.opentheso.v2.concept.model.FacetMemberItem;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.ui.WriteUiMessages;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
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
import java.util.Map;
import java.util.Set;

/**
 * Édition inline des membres de la fiche facette.
 */
@Getter
@Setter
@ViewScoped
@Named("v2FacetMemberBlockEditorBean")
@RequiredArgsConstructor
public class FacetMemberBlockEditorBean implements Serializable {

    static final String FICHE_CARD = "f-mem";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient FacetMutationService facetMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingFacetId;
    private List<MemberEditRow> rows = new ArrayList<>();
    private String membersPayload = "";
    private String errorMessage;
    private String flashMessage;
    private String flashToken;
    private boolean treeReload;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedFacet() != null
                && conceptWritePolicy.canMutateHierarchicalRelations(userSession, false);
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
        rows = copyRows(facet);
        membersPayload = serializeRows(rows);
        errorMessage = "";
        flashMessage = "";
        flashToken = "";
        treeReload = false;
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
        if (userSession.getCurrentUserId() == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        FacetDetailOverview current = thesaurusViewBean.getSelectedFacet();
        if (current == null || StringUtils.isBlank(current.facetId())) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        applyPayloadToRows();
        persistMemberChanges(current);
    }

    private void persistMemberChanges(FacetDetailOverview current) {
        String thesaurusId = thesaurusViewBean.getId();
        String facetId = current.facetId();
        Map<String, String> oldById = membersById(current);
        Map<String, String> selected = selectedById();
        boolean dirty = false;
        for (String conceptId : oldById.keySet()) {
            if (selected.containsKey(conceptId)) {
                continue;
            }
            MutationResult deleted = facetMutationService.removeMember(
                    new RemoveFacetMemberCommand(thesaurusId, facetId, conceptId, false));
            if (!applyResult(deleted, dirty)) {
                return;
            }
            dirty = true;
        }
        for (String conceptId : selected.keySet()) {
            if (oldById.containsKey(conceptId)) {
                continue;
            }
            MutationResult added = facetMutationService.addMember(
                    new AddFacetMemberCommand(thesaurusId, facetId, conceptId, false));
            if (!applyResult(added, dirty)) {
                return;
            }
            dirty = true;
        }
        finishSuccess(dirty);
    }

    private Map<String, String> selectedById() {
        Map<String, String> selected = new LinkedHashMap<>();
        for (MemberEditRow row : rows) {
            if (row == null || StringUtils.isBlank(row.getConceptId())) {
                continue;
            }
            selected.putIfAbsent(row.getConceptId().trim(), StringUtils.defaultString(row.getLabel()));
        }
        return selected;
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

    private void finishSuccess(boolean dirty) {
        editing = false;
        if (FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        errorMessage = "";
        flashMessage = "Membres enregistrés";
        flashToken = String.valueOf(System.currentTimeMillis());
        treeReload = dirty;
        thesaurusViewBean.reloadSelectedConcept();
        if (dirty) {
            thesaurusViewBean.reloadTree();
        }
    }

    private void reloadIfDirty(boolean dirty) {
        if (dirty) {
            thesaurusViewBean.reloadSelectedConcept();
            thesaurusViewBean.reloadTree();
        }
    }

    private void resetForm(boolean keepFlash) {
        editing = false;
        if (FICHE_CARD.equals(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        editingFacetId = null;
        rows = new ArrayList<>();
        membersPayload = "";
        errorMessage = "";
        treeReload = false;
        if (!keepFlash) {
            flashMessage = "";
            flashToken = "";
        }
    }

    private boolean matchesCurrentFacet() {
        FacetDetailOverview facet = thesaurusViewBean.getSelectedFacet();
        return facet != null && Strings.CS.equals(editingFacetId, facet.facetId());
    }

    private static List<MemberEditRow> copyRows(FacetDetailOverview facet) {
        List<MemberEditRow> copied = new ArrayList<>();
        if (facet == null || facet.members() == null) {
            return copied;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (FacetMemberItem member : facet.members()) {
            if (member == null || StringUtils.isBlank(member.conceptId()) || !seen.add(member.conceptId())) {
                continue;
            }
            copied.add(new MemberEditRow(member.conceptId(), StringUtils.defaultString(member.label())));
        }
        return copied;
    }

    private static Map<String, String> membersById(FacetDetailOverview facet) {
        Map<String, String> byId = new LinkedHashMap<>();
        if (facet == null || facet.members() == null) {
            return byId;
        }
        for (FacetMemberItem member : facet.members()) {
            if (member == null || StringUtils.isBlank(member.conceptId())) {
                continue;
            }
            byId.putIfAbsent(member.conceptId(), StringUtils.defaultString(member.label()));
        }
        return byId;
    }

    static String serializeRows(List<MemberEditRow> source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        for (MemberEditRow row : source) {
            if (row == null || StringUtils.isBlank(row.getConceptId())) {
                continue;
            }
            lines.add(row.getConceptId().trim() + "\t" + StringUtils.defaultString(row.getLabel()));
        }
        return String.join("\n", lines);
    }

    void applyPayloadToRows() {
        List<MemberEditRow> parsed = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String line : StringUtils.defaultString(membersPayload).split("\\R")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            int tab = line.indexOf('\t');
            String conceptId = (tab < 0 ? line : line.substring(0, tab)).trim();
            String label = tab < 0 ? "" : line.substring(tab + 1).trim();
            if (conceptId.isEmpty() || !seen.add(conceptId)) {
                continue;
            }
            parsed.add(new MemberEditRow(conceptId, label));
        }
        rows = parsed;
    }

    @Getter
    @Setter
    public static class MemberEditRow implements Serializable {
        private String conceptId;
        private String label;

        public MemberEditRow() {
        }

        public MemberEditRow(String conceptId, String label) {
            this.conceptId = conceptId;
            this.label = label;
        }
    }
}
