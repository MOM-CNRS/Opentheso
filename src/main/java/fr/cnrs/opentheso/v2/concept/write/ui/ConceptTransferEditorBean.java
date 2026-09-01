package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteThesaurusOption;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.MoveConceptToThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
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

@Getter
@Setter
@ViewScoped
@Named("v2ConceptTransferEditorBean")
@RequiredArgsConstructor
public class ConceptTransferEditorBean implements Serializable {

    private final ConceptTransferMutationService conceptTransferMutationService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final BranchConceptSupport branchConceptSupport;

    private String sourceThesaurusLabel;
    private String sourceLabel;
    private String targetThesaurusId;
    private String destMode = "";
    private String parentConceptId;
    private String parentLabel;
    private String errorMessage;
    private String flashMessage;
    private String flashToken;
    private List<String> branchConceptIds = Collections.emptyList();
    private List<ConceptWriteThesaurusOption> availableThesauri = Collections.emptyList();

    public boolean isTransferActionsAvailable() {
        return conceptWritePolicy.canTransferConcept(userSession);
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

    public void prepareMoveToAnotherThesaurus() {
        errorMessage = null;
        destMode = "";
        parentConceptId = "";
        parentLabel = "";
        targetThesaurusId = null;
        if (!conceptSelectionContext.hasSelection()) {
            branchConceptIds = Collections.emptyList();
            availableThesauri = Collections.emptyList();
            sourceLabel = "";
            sourceThesaurusLabel = "";
            return;
        }
        sourceThesaurusLabel = StringUtils.defaultIfBlank(
                thesaurusContext.getCurrentThesaurusTitle(),
                thesaurusContext.resolveThesaurusId());
        sourceLabel = conceptSelectionContext.getSummary().preferredLabel();
        branchConceptIds = branchConceptSupport.collectBranchConceptIds(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId());
        loadAvailableThesauri();
    }

    public void onTargetThesaurusChange() {
        destMode = "";
        parentConceptId = "";
        parentLabel = "";
        errorMessage = null;
    }

    public boolean submitMoveToAnotherThesaurus() {
        errorMessage = null;
        if (!isTransferActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            errorMessage = "Action non autorisée";
            return false;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || StringUtils.isBlank(targetThesaurusId) || branchConceptIds.isEmpty()) {
            errorMessage = "Choisissez un thésaurus de destination";
            return false;
        }
        if (!"root".equals(destMode) && !"parent".equals(destMode)) {
            errorMessage = "Choisissez un emplacement";
            return false;
        }
        boolean toRoot = "root".equals(destMode);
        String parentId = toRoot ? null : parentConceptId;
        if (!toRoot && StringUtils.isBlank(parentId)) {
            errorMessage = "Choisissez un concept parent, ou la racine";
            return false;
        }
        var command = new MoveConceptToThesaurusCommand(
                thesaurusContext.resolveThesaurusId(),
                targetThesaurusId,
                conceptSelectionContext.getConceptId(),
                branchConceptIds,
                thesaurusContext.resolveWorkLanguage(),
                userId,
                StringUtils.defaultString(userSession.getCurrentUsername()),
                parentId
        );
        try {
            MutationResult result = conceptTransferMutationService.moveConceptToThesaurus(command);
            if (result == null || !result.success()) {
                errorMessage = result != null ? result.message() : "Le déplacement a échoué";
                return false;
            }
            conceptNavigationSupport.invalidateConceptTree();
            conceptNavigationSupport.openThesaurusHome();
            flashSuccess(StringUtils.defaultIfBlank(result.message(),
                    sourceLabel + " → " + getTargetThesaurusLabel()));
            return true;
        } catch (RuntimeException exception) {
            errorMessage = "Le déplacement a échoué";
            return false;
        }
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
