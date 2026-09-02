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

    private final ConceptAttributeMutationService conceptAttributeMutationService;
    private final ConceptWriteMetadataService conceptWriteMetadataService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentConceptLabel;
    private String notation;
    private String selectedConceptType;
    private String currentConceptTypeCode;
    private String currentConceptTypeLabel;
    private boolean applyConceptTypeToBranch;
    private boolean appliedToBranch;
    private String typeRunState = "";
    private String typeErrorMessage;
    private String typeFlashMessage;
    private String typeFlashToken;
    private List<ConceptWriteConceptType> availableConceptTypes = Collections.emptyList();

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
        return "done".equals(typeRunState);
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
        typeRunState = "";
        typeErrorMessage = null;
        typeFlashMessage = null;
        typeFlashToken = null;
        availableConceptTypes = conceptWriteMetadataService.listConceptTypes(thesaurusContext.resolveThesaurusId());
        currentConceptTypeCode = currentConceptType();
        selectedConceptType = currentConceptTypeCode;
        currentConceptTypeLabel = labelForCode(currentConceptTypeCode);
    }

    public void selectConceptType(String code) {
        if (isTypeDone()) {
            return;
        }
        typeErrorMessage = null;
        selectedConceptType = StringUtils.trimToEmpty(code);
    }

    public void submitUpdateNotation() {
        Integer userId = requireUserId();
        if (userId == null || !isNotationEditAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
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
        typeErrorMessage = null;
        typeFlashMessage = null;
        if (isTypeDone()) {
            return;
        }
        Integer userId = requireUserId();
        if (userId == null || !isConceptTypeEditAvailable() || !conceptSelectionContext.hasSelection()) {
            typeRunState = "error";
            typeErrorMessage = "Action non autorisée";
            return;
        }
        if (StringUtils.isBlank(selectedConceptType)) {
            typeRunState = "error";
            typeErrorMessage = "Choisissez un type";
            return;
        }
        if (isSameAsCurrent() && !applyConceptTypeToBranch) {
            typeErrorMessage = "Ce concept a déjà ce type";
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
            typeRunState = "error";
            typeErrorMessage = result != null ? result.message() : "Erreur";
            return;
        }
        typeRunState = "done";
        appliedToBranch = applyConceptTypeToBranch;
        currentConceptTypeCode = selectedConceptType;
        currentConceptTypeLabel = labelForCode(selectedConceptType);
        typeFlashMessage = appliedToBranch
                ? "Type « " + currentConceptTypeLabel + " » appliqué à la branche"
                : "Type « " + currentConceptTypeLabel + " » appliqué";
        typeFlashToken = String.valueOf(System.currentTimeMillis());
    }

    public void finishTypeAfterClose() {
        typeFlashMessage = null;
        typeFlashToken = null;
        typeErrorMessage = null;
        typeRunState = "";
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
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
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
        return "dep".equalsIgnoreCase(org.apache.commons.lang3.StringUtils.trimToEmpty(
                conceptSelectionContext.getSummary().status()));
    }

    private Integer requireUserId() {
        return userSession.getCurrentUserId();
    }

    private String contributorName() {
        return org.apache.commons.lang3.StringUtils.defaultString(userSession.getCurrentUsername());
    }
}
