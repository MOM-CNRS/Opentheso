package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.alignment.ui.ConceptAlignmentAdminBean;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAlignmentMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.ObjectProvider;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Bean d'écriture des alignements SKOS d'un concept (ajout/édition/suppression manuels).
 * Repose uniquement sur {@link ConceptAlignmentMutationService}, sans passer par le bean
 * ou le service legacy d'alignement.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptAlignmentBean")
@RequiredArgsConstructor
public class ConceptAlignmentEditorBean implements Serializable {

    private final ConceptAlignmentMutationService conceptAlignmentMutationService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ObjectProvider<ConceptAlignmentAdminBean> conceptAlignmentAdminBean;

    private List<ConceptWriteAlignmentType> alignmentTypes = Collections.emptyList();

    private int manualAlignmentType = -1;
    private String manualAlignmentUri;
    private String manualAlignmentSource;

    private int editingAlignmentId = -1;
    private int editAlignmentType = -1;
    private String editAlignmentUri;
    private String editAlignmentSource;

    private int alignmentToDeleteId = -1;

    public boolean isManagerActionsAvailable() {
        return conceptWritePolicy.canMutateAlignments(userSession, isSelectedDeprecated());
    }

    public void prepareManualAlignment() {
        loadAlignmentTypes();
        manualAlignmentType = alignmentTypes.isEmpty() ? -1 : alignmentTypes.get(0).getId();
        manualAlignmentUri = "";
        manualAlignmentSource = "";
    }

    public void prepareEdit(ConceptAlignment alignment) {
        loadAlignmentTypes();
        if (alignment == null) {
            editingAlignmentId = -1;
            return;
        }
        editingAlignmentId = parseAlignmentId(alignment.getId());
        editAlignmentType = alignment.getTypeId();
        editAlignmentUri = alignment.getUri();
        editAlignmentSource = alignment.getSourceName();
    }

    public void prepareDelete(ConceptAlignment alignment) {
        alignmentToDeleteId = alignment == null ? -1 : parseAlignmentId(alignment.getId());
    }

    public void addManualAlignment() {
        if (!isManagerActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new AddManualAlignmentCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                manualAlignmentType,
                manualAlignmentUri,
                manualAlignmentSource,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptAlignmentMutationService.addManualAlignment(command),
                summary.conceptId(),
                "PF('v2AddManualAlignment').hide();"
        );
    }

    public void updateAlignment() {
        if (!isManagerActionsAvailable() || !conceptSelectionContext.hasSelection() || editingAlignmentId <= 0) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new UpdateAlignmentCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                editingAlignmentId,
                editAlignmentType,
                editAlignmentUri,
                editAlignmentSource,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptAlignmentMutationService.updateAlignment(command),
                summary.conceptId(),
                "PF('v2EditAlignment').hide();"
        );
    }

    public void deleteAlignment() {
        if (!isManagerActionsAvailable() || !conceptSelectionContext.hasSelection() || alignmentToDeleteId <= 0) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var command = new DeleteAlignmentCommand(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                alignmentToDeleteId,
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername())
        );
        handleMutationResult(
                conceptAlignmentMutationService.deleteAlignment(command),
                summary.conceptId(),
                "PF('v2DeleteAlignment').hide();"
        );
    }

    private void handleMutationResult(MutationResult result, String conceptId, String hideDialogScript) {
        if (result == null) {
            return;
        }
        switch (result.outcome()) {
            case OK -> {
                ConceptAlignmentAdminBean adminBean = conceptAlignmentAdminBean.getIfAvailable();
                String previousBranchRoot = adminBean != null ? adminBean.getRootConceptId() : null;
                conceptNavigationSupport.openConcept(conceptId);
                if (adminBean != null) {
                    if (StringUtils.isNotBlank(previousBranchRoot)) {
                        adminBean.reloadBranchSummary(previousBranchRoot);
                    } else {
                        adminBean.reloadCurrentBranchSummary();
                    }
                }
                PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
                if (result.warning()) {
                    MessageUtils.showWarnMessage(result.message());
                } else {
                    MessageUtils.showInformationMessage(result.message());
                }
                if (StringUtils.isNotBlank(hideDialogScript)) {
                    PrimeFaces.current().executeScript(hideDialogScript);
                }
            }
            case VALIDATION_ERROR, DUPLICATE_LABEL, FAILURE, FORBIDDEN -> MessageUtils.showErrorMessage(result.message());
        }
    }

    private void loadAlignmentTypes() {
        alignmentTypes = conceptAlignmentMutationService.listAlignmentTypes();
    }

    private int parseAlignmentId(String rawId) {
        if (StringUtils.isBlank(rawId) || !StringUtils.isNumeric(rawId)) {
            return -1;
        }
        return Integer.parseInt(rawId);
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(conceptSelectionContext.getSummary().status()));
    }
}
