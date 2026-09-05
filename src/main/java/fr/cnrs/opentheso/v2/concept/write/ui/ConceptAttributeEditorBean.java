package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptCustomRelationItem;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteConceptType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNotationCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptAttributeMutationService;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteMetadataService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptAttributeEditorBean")
@RequiredArgsConstructor
public class ConceptAttributeEditorBean implements Serializable {

    private final transient ConceptAttributeMutationService conceptAttributeMutationService;
    private final transient ConceptWriteMetadataService conceptWriteMetadataService;
    private final transient ConceptSelectionContext conceptSelectionContext;
    private final transient ConceptNavigationSupport conceptNavigationSupport;
    private final transient ThesaurusContext thesaurusContext;
    private final transient UserSession userSession;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient ThesaurusBrowseBean thesaurusBrowseBean;
    private final transient V2LocaleBean v2LocaleBean;

    private final DialogRunState typeRun = new DialogRunState();

    private String currentConceptLabel;
    private String notation;
    private String selectedConceptType;
    private String currentConceptTypeCode;
    private String currentConceptTypeLabel;
    private boolean applyConceptTypeToBranch;
    private boolean appliedToBranch;
    private List<ConceptWriteConceptType> availableConceptTypes = Collections.emptyList();

    public String getTypeRunState() {
        return typeRun.getState();
    }

    public String getTypeErrorMessage() {
        return typeRun.getErrorMessage();
    }

    public String getTypeFlashMessage() {
        return typeRun.getFlashMessage();
    }

    public String getTypeFlashToken() {
        return typeRun.getFlashToken();
    }

    public boolean isAttributeActionsAvailable() {
        return conceptWritePolicy.canMutateConceptAttributes(userSession, isSelectedDeprecated());
    }

    public boolean isNotationEditAvailable() {
        return isAttributeActionsAvailable();
    }

    public boolean isConceptTypeEditAvailable() {
        return isAttributeActionsAvailable()
                && thesaurusBrowseBean.isCustomRelationVisible()
                && !hasOutgoingCustomRelations();
    }

    public void prepareEditNotation() {
        refreshCurrentConceptLabel();
        notation = currentNotation();
    }

    public boolean isTypeDone() {
        return typeRun.isDone();
    }

    public boolean isTypeApplyReady() {
        return !isTypeDone()
                && isConceptTypeEditAvailable()
                && conceptSelectionContext.hasSelection()
                && StringUtils.isNotBlank(selectedConceptType);
    }

    public boolean isTypePicked(ConceptWriteConceptType type) {
        return type != null && normalizeTypeCode(type.code()).equalsIgnoreCase(normalizeTypeCode(selectedConceptType));
    }

    public boolean isTypeCurrent(ConceptWriteConceptType type) {
        return type != null && normalizeTypeCode(type.code()).equalsIgnoreCase(normalizeTypeCode(currentConceptTypeCode));
    }

    public void prepareEditConceptType() {
        refreshCurrentConceptLabel();
        applyConceptTypeToBranch = false;
        appliedToBranch = false;
        typeRun.reset();
        availableConceptTypes = conceptWriteMetadataService.listConceptTypes(thesaurusContext.resolveThesaurusId());
        currentConceptTypeCode = currentConceptType();
        selectedConceptType = currentConceptTypeCode;
        currentConceptTypeLabel = labelForCode(currentConceptTypeCode);
    }

    public void selectConceptType(String code) {
        if (isTypeDone()) {
            return;
        }
        typeRun.setErrorMessage(null);
        selectedConceptType = StringUtils.trimToEmpty(code);
    }

    public void submitUpdateNotation() {
        Integer userId = requireUserId();
        if (userId == null || !isNotationEditAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage(unauthorized());
            return;
        }
        var command = new UpdateNotationCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                notation
        );
        if (handleNotationMutationResult(conceptAttributeMutationService.updateNotation(command))) {
            notation = StringUtils.trimToEmpty(notation);
        }
    }

    public void submitUpdateConceptType() {
        typeRun.setErrorMessage(null);
        typeRun.clearFlash();
        if (isTypeDone()) {
            return;
        }
        Integer userId = requireUserId();
        if (userId == null || !isConceptTypeEditAvailable() || !conceptSelectionContext.hasSelection()) {
            typeRun.fail(unauthorized());
            return;
        }
        if (StringUtils.isBlank(selectedConceptType)) {
            typeRun.fail(msg("v2.concept.editTypeChoose", "Choisissez un type"));
            return;
        }
        if (isSameAsCurrent() && !applyConceptTypeToBranch) {
            typeRun.setErrorMessage(msg("v2.concept.editTypeSame", "Ce concept a déjà ce type"));
            return;
        }
        var command = new UpdateConceptTypeCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                selectedConceptType,
                applyConceptTypeToBranch
        );
        MutationResult result = conceptAttributeMutationService.updateConceptType(command);
        if (result == null || !result.success()) {
            typeRun.fail(result != null ? result.message() : msg("v2.write.failed", "Erreur"));
            return;
        }
        appliedToBranch = applyConceptTypeToBranch;
        currentConceptTypeCode = selectedConceptType;
        currentConceptTypeLabel = labelForCode(selectedConceptType);
        typeRun.succeed(appliedToBranch
                ? msg("v2.concept.editTypeAppliedBranch", "Type « {0} » appliqué à la branche", currentConceptTypeLabel)
                : msg("v2.concept.editTypeApplied", "Type « {0} » appliqué", currentConceptTypeLabel));
    }

    public void finishTypeAfterClose() {
        typeRun.reset();
        applyConceptTypeToBranch = false;
        appliedToBranch = false;
    }

    public String formatConceptTypeOption(ConceptWriteConceptType type) {
        if (type == null) {
            return "";
        }
        String reciprocal = type.reciprocal() ? " (R)" : "";
        return formatConceptTypeLabel(type) + " (" + type.code() + reciprocal + ")";
    }

    public String formatConceptTypeLabel(ConceptWriteConceptType type) {
        if (type == null) {
            return "";
        }
        String label = "fr".equalsIgnoreCase(thesaurusContext.resolveWorkLanguage())
                ? type.labelFr()
                : type.labelEn();
        if (StringUtils.isBlank(label)) {
            label = StringUtils.defaultIfBlank(type.labelEn(), type.labelFr());
        }
        return StringUtils.defaultIfBlank(label, type.code());
    }

    private boolean handleNotationMutationResult(MutationResult result) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : msg("v2.write.failed", "Erreur"));
            return false;
        }
        String conceptId = conceptSelectionContext.getConceptId();
        String updatedNotation = StringUtils.trimToEmpty(notation);
        conceptNavigationSupport.refreshAfterNotationUpdate(conceptId, updatedNotation);
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :containerIndex:tabTree :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        PrimeFaces.current().executeScript("PF('v2EditNotationDlg').hide();");
        return true;
    }

    private boolean hasOutgoingCustomRelations() {
        if (thesaurusBrowseBean.getSelectedConcept() == null) {
            return false;
        }
        List<ConceptCustomRelationItem> relations = thesaurusBrowseBean.getSelectedConcept().outgoingCustomRelations();
        return relations != null && !relations.isEmpty();
    }

    private String currentNotation() {
        if (thesaurusBrowseBean.getSelectedConcept() == null
                || thesaurusBrowseBean.getSelectedConcept().summary() == null) {
            return "";
        }
        return StringUtils.defaultString(thesaurusBrowseBean.getSelectedConcept().summary().notation());
    }

    private String currentConceptType() {
        if (conceptSelectionContext.hasSelection() && conceptSelectionContext.getSummary() != null) {
            return normalizeTypeCode(conceptSelectionContext.getSummary().conceptType());
        }
        if (thesaurusBrowseBean.getSelectedConcept() == null
                || thesaurusBrowseBean.getSelectedConcept().summary() == null) {
            return "concept";
        }
        return normalizeTypeCode(thesaurusBrowseBean.getSelectedConcept().summary().conceptType());
    }

    private boolean isSameAsCurrent() {
        return normalizeTypeCode(selectedConceptType).equalsIgnoreCase(normalizeTypeCode(currentConceptTypeCode));
    }

    private String normalizeTypeCode(String code) {
        return StringUtils.isBlank(code) ? "concept" : code.trim();
    }

    private String labelForCode(String code) {
        String normalized = normalizeTypeCode(code);
        if (availableConceptTypes != null) {
            for (ConceptWriteConceptType type : availableConceptTypes) {
                if (type != null && normalized.equalsIgnoreCase(normalizeTypeCode(type.code()))) {
                    return formatConceptTypeLabel(type);
                }
            }
        }
        return normalized;
    }

    private void refreshCurrentConceptLabel() {
        currentConceptLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(
                conceptSelectionContext.getSummary().status()));
    }

    private Integer requireUserId() {
        return userSession.getCurrentUserId();
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }


    private String unauthorized() {
        return WriteUiMessages.unauthorized(v2LocaleBean);
    }

    private String msg(String key, String fallback) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback);
    }

    private String msg(String key, String fallback, Object... args) {
        return WriteUiMessages.msg(v2LocaleBean, key, fallback, args);
    }
}
