package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.CopyBranchBetweenThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptCopyBetweenThesaurusMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptTransferMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Copie de branche vers un autre thésaurus, en une seule fenêtre.
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptCopyBetweenThesaurusBean")
@RequiredArgsConstructor
public class ConceptCopyBetweenThesaurusBean implements Serializable {

    private final transient ConceptCopyBetweenThesaurusMutationService conceptCopyBetweenThesaurusMutationService;
    private final transient ConceptTransferMutationService conceptTransferMutationService;
    private final transient BranchConceptSupport branchConceptSupport;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;

    private String sourceThesaurusId;
    private String sourceThesaurusLabel;
    private String sourceConceptId;
    private String sourceLabel;
    private String targetThesaurusId;
    private String destMode = "";
    private String parentConceptId;
    private String parentLabel;
    private String identifierType = "sans";
    private String errorMessage;
    private String flashMessage;
    private String flashToken;
    private List<String> conceptsToCopy = Collections.emptyList();
    private List<ConceptWriteThesaurusOption> availableThesauri = Collections.emptyList();

    public boolean isCopyActionsAvailable() {
        return conceptWritePolicy.canMutateHierarchicalRelations(userSession, false);
    }

    public boolean isTargetThesaurusSelected() {
        return StringUtils.isNotBlank(targetThesaurusId);
    }

    public boolean isParentSelected() {
        return StringUtils.isNotBlank(parentConceptId);
    }

    public boolean isSubmitReady() {
        if (!isTargetThesaurusSelected()) {
            return false;
        }
        if ("root".equals(destMode)) {
            return true;
        }
        return "parent".equals(destMode) && isParentSelected();
    }

    public String getTargetThesaurusLabel() {
        if (StringUtils.isBlank(targetThesaurusId)) {
            return "";
        }
        return availableThesauri.stream()
                .filter(th -> targetThesaurusId.equalsIgnoreCase(th.id()))
                .map(th -> StringUtils.defaultIfBlank(th.title(), th.id()) + " (" + th.id() + ")")
                .findFirst()
                .orElse(targetThesaurusId);
    }

    public void prepareCopyToAnotherThesaurus() {
        errorMessage = null;
        destMode = "";
        parentConceptId = "";
        parentLabel = "";
        identifierType = "sans";
        targetThesaurusId = null;
        if (!isCopyActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            conceptsToCopy = Collections.emptyList();
            availableThesauri = Collections.emptyList();
            sourceLabel = "";
            sourceThesaurusLabel = "";
            return;
        }
        sourceThesaurusId = thesaurusContext.resolveThesaurusId();
        sourceThesaurusLabel = StringUtils.defaultIfBlank(
                thesaurusContext.getCurrentThesaurusTitle(), sourceThesaurusId);
        sourceConceptId = conceptSelectionContext.getConceptId();
        sourceLabel = conceptSelectionContext.getSummary().preferredLabel();
        conceptsToCopy = branchConceptSupport.collectBranchConceptIds(sourceThesaurusId, sourceConceptId);
        loadAvailableThesauri();
    }

    public void onTargetThesaurusChange() {
        destMode = "";
        parentConceptId = "";
        parentLabel = "";
        errorMessage = null;
    }

    public boolean submitCopyToAnotherThesaurus() {
        errorMessage = null;
        String validationError = validateCopyRequest();
        if (validationError != null) {
            errorMessage = validationError;
            return false;
        }
        boolean toRoot = "root".equals(destMode);
        String parentId = toRoot ? null : parentConceptId;
        MutationResult valid = conceptCopyBetweenThesaurusMutationService.validateIdsAvailable(
                targetThesaurusId, conceptsToCopy);
        if (valid == null || !valid.success()) {
            errorMessage = valid != null ? valid.message() : "La copie a échoué";
            return false;
        }
        MutationResult result = conceptCopyBetweenThesaurusMutationService.copyBranch(
                new CopyBranchBetweenThesaurusCommand(
                        sourceThesaurusId,
                        sourceConceptId,
                        targetThesaurusId,
                        parentId,
                        toRoot,
                        StringUtils.defaultIfBlank(identifierType, "sans"),
                        userSession.getCurrentUserId()
                ));
        if (result == null || !result.success()) {
            errorMessage = result != null ? result.message() : "La copie a échoué";
            return false;
        }
        flashSuccess(StringUtils.defaultIfBlank(result.message(),
                sourceLabel + " → " + getTargetThesaurusLabel()));
        return true;
    }

    private String validateCopyRequest() {
        if (!isCopyActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            return WriteUiMessages.UNAUTHORIZED_FALLBACK;
        }
        if (userSession.getCurrentUserId() == null
                || StringUtils.isBlank(targetThesaurusId)
                || conceptsToCopy.isEmpty()) {
            return "Choisissez un thésaurus de destination";
        }
        if (!"root".equals(destMode) && !"parent".equals(destMode)) {
            return "Choisissez un emplacement";
        }
        if (!"root".equals(destMode) && StringUtils.isBlank(parentConceptId)) {
            return "Choisissez un concept parent, ou la racine";
        }
        return null;
    }

    private void loadAvailableThesauri() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            availableThesauri = Collections.emptyList();
            return;
        }
        availableThesauri = conceptTransferMutationService.listAdminThesauri(
                userId,
                userSession.isSuperAdmin(),
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    private void flashSuccess(String message) {
        flashMessage = message;
        flashToken = String.valueOf(System.currentTimeMillis());
    }
}
