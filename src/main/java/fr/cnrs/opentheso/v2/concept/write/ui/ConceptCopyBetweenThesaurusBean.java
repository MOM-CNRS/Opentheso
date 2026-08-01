package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.CopyBranchBetweenThesaurusCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptCopyBetweenThesaurusMutationService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Copie de branche entre thésaurus — équivalent V2 de {@code copyAndPasteBetweenThesaurus}.
 */
@Getter
@Setter
@SessionScoped
@Named("v2ConceptCopyBetweenThesaurusBean")
@RequiredArgsConstructor
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ConceptCopyBetweenThesaurusBean implements Serializable {

    private final ConceptCopyBetweenThesaurusMutationService conceptCopyBetweenThesaurusMutationService;
    private final BranchConceptSupport branchConceptSupport;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ObjectProvider<ThesaurusBrowseBean> thesaurusBrowseBean;

    private boolean copyOn;
    private boolean dropToRoot;
    private String sourceThesaurusId;
    private String sourceConceptId;
    private String sourceLabel;
    private List<String> conceptsToCopy = Collections.emptyList();
    private String identifierType = "sans";

    public boolean isCopyActionsAvailable() {
        return conceptWritePolicy.canMutateHierarchicalRelations(userSession, false);
    }

    public boolean isPasteAvailable() {
        if (!copyOn || !isCopyActionsAvailable()) {
            return false;
        }
        String currentTheso = thesaurusContext.resolveThesaurusId();
        return StringUtils.isNotBlank(currentTheso)
                && !currentTheso.equalsIgnoreCase(sourceThesaurusId);
    }

    public boolean isPasteUnderConceptAvailable() {
        return isPasteAvailable() && conceptSelectionContext.hasSelection();
    }

    public void onStartCopy() {
        if (!isCopyActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        reset();
        sourceThesaurusId = thesaurusContext.resolveThesaurusId();
        sourceConceptId = conceptSelectionContext.getConceptId();
        sourceLabel = conceptSelectionContext.getSummary().preferredLabel();
        conceptsToCopy = branchConceptSupport.collectBranchConceptIds(sourceThesaurusId, sourceConceptId);
        copyOn = true;
        MessageUtils.showInformationMessage(
                "Copier " + StringUtils.defaultString(sourceLabel)
                        + " (" + sourceConceptId + ") Total = " + conceptsToCopy.size());
    }

    public void preparePasteUnderCurrentConcept() {
        dropToRoot = false;
        if (!validatePaste()) {
            return;
        }
        PrimeFaces.current().ajax().update(":containerIndex:v2CopyBetweenThesoForm");
        PrimeFaces.current().executeScript("PF('v2CopyBetweenThesoDlg').show();");
    }

    public void preparePasteAtRoot() {
        dropToRoot = true;
        if (!validatePaste()) {
            return;
        }
        PrimeFaces.current().ajax().update(":containerIndex:v2CopyBetweenThesoToRootForm");
        PrimeFaces.current().executeScript("PF('v2CopyBetweenThesoToRootDlg').show();");
    }

    public boolean validatePaste() {
        if (!isPasteAvailable()) {
            return false;
        }
        MutationResult result = conceptCopyBetweenThesaurusMutationService.validateIdsAvailable(
                thesaurusContext.resolveThesaurusId(), conceptsToCopy);
        if (!result.success()) {
            MessageUtils.showErrorMessage(result.message());
            return false;
        }
        return true;
    }

    public void paste() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || !copyOn) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        String targetTheso = thesaurusContext.resolveThesaurusId();
        String targetParent = dropToRoot || !conceptSelectionContext.hasSelection()
                ? null
                : conceptSelectionContext.getConceptId();
        if (!dropToRoot && StringUtils.isBlank(targetParent)) {
            MessageUtils.showErrorMessage("Aucune sélection !");
            return;
        }

        MutationResult result = conceptCopyBetweenThesaurusMutationService.copyBranch(
                new CopyBranchBetweenThesaurusCommand(
                        sourceThesaurusId,
                        sourceConceptId,
                        targetTheso,
                        targetParent,
                        dropToRoot,
                        identifierType,
                        userId
                ));
        if (!result.success()) {
            MessageUtils.showErrorMessage(result.message());
            return;
        }

        String message = dropToRoot
                ? sourceLabel + " -> Root"
                : sourceLabel + " -> " + conceptSelectionContext.getSummary().preferredLabel();
        String openedId = sourceConceptId;
        reset();

        ThesaurusBrowseBean browse = thesaurusBrowseBean.getIfAvailable();
        if (browse != null) {
            browse.invalidateConceptTree();
            browse.invalidateCollectionTree();
            browse.openConcept(openedId, true);
        }
        PrimeFaces.current().executeScript(
                "PF('v2CopyBetweenThesoDlg') && PF('v2CopyBetweenThesoDlg').hide();"
                        + "PF('v2CopyBetweenThesoToRootDlg') && PF('v2CopyBetweenThesoToRootDlg').hide();");
        PrimeFaces.current().ajax().update(
                ":containerIndex:formLeftTab",
                ":containerIndex:formRightTab",
                ":messageIndex"
        );
        MessageUtils.showInformationMessage(message);
    }

    public void cancelCopy() {
        reset();
        MessageUtils.showInformationMessage("Copie annulée");
    }

    public void reset() {
        copyOn = false;
        dropToRoot = false;
        sourceThesaurusId = null;
        sourceConceptId = null;
        sourceLabel = null;
        conceptsToCopy = Collections.emptyList();
        identifierType = "sans";
    }
}
