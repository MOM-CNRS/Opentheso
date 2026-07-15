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
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentConceptLabel;
    private String notation;
    private String selectedConceptType;
    private boolean applyConceptTypeToBranch;
    private List<ConceptWriteConceptType> availableConceptTypes = Collections.emptyList();

    public boolean isAttributeActionsAvailable() {
        return ConceptWritePolicy.canMutateConceptAttributes(userSession, isSelectedDeprecated());
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

    public void prepareEditConceptType() {
        refreshCurrentConceptLabel();
        applyConceptTypeToBranch = false;
        availableConceptTypes = conceptWriteMetadataService.listConceptTypes(thesaurusContext.resolveThesaurusId());
        selectedConceptType = currentConceptType();
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
        Integer userId = requireUserId();
        if (userId == null || !isConceptTypeEditAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
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
        handleMutationResult(conceptAttributeMutationService.updateConceptType(command), "v2EditConceptTypeDlg");
    }

    public String formatConceptTypeOption(ConceptWriteConceptType type) {
        if (type == null) {
            return "";
        }
        String label = "fr".equalsIgnoreCase(thesaurusContext.resolveWorkLanguage())
                ? type.labelFr()
                : type.labelEn();
        if (StringUtils.isBlank(label)) {
            label = StringUtils.defaultIfBlank(type.labelEn(), type.labelFr());
        }
        String reciprocal = type.reciprocal() ? " (R)" : "";
        return label + " (" + type.code() + reciprocal + ")";
    }

    private boolean handleNotationMutationResult(MutationResult result) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return false;
        }
        String conceptId = conceptSelectionContext.getConceptId();
        String updatedNotation = StringUtils.trimToEmpty(notation);
        conceptNavigationSupport.refreshAfterNotationUpdate(conceptId, updatedNotation);
        PrimeFaces.current().ajax().update(":containerIndex:containerIndex:conceptSummaryPanel :containerIndex:tabTree :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        PrimeFaces.current().executeScript("PF('v2EditNotationDlg').hide();");
        return true;
    }

    private boolean handleMutationResult(MutationResult result, String dialogWidget) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return false;
        }
        conceptNavigationSupport.refreshSelectedConcept();
        PrimeFaces.current().ajax().update(":containerIndex:conceptSummaryPanel :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(dialogWidget)) {
            PrimeFaces.current().executeScript("PF('" + dialogWidget + "').hide();");
        }
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
        if (thesaurusBrowseBean.getSelectedConcept() == null
                || thesaurusBrowseBean.getSelectedConcept().summary() == null) {
            return "";
        }
        return StringUtils.defaultString(thesaurusBrowseBean.getSelectedConcept().summary().conceptType());
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
