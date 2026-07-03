package fr.cnrs.opentheso.v2.concept.ui;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignmentType;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.candidat.session.CandidatAlignmentSupport;
import fr.cnrs.opentheso.v2.concept.model.ConceptAlignment;
import fr.cnrs.opentheso.v2.concept.service.ConceptAlignmentMutationService;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
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
@Named("v2ConceptAlignmentBean")
@RequiredArgsConstructor
public class ConceptAlignmentBean implements Serializable {

    private final ConceptAlignmentMutationService conceptAlignmentMutationService;
    private final CandidatAlignmentSupport candidatAlignmentSupport;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final V2LocaleBean v2LocaleBean;

    private List<NodeAlignmentType> alignmentTypes = Collections.emptyList();
    private ConceptAlignment alignmentToDelete;
    private ConceptAlignment alignmentToEdit;
    private int manualAlignmentType = -1;
    private String manualAlignmentUri;
    private String manualAlignmentSource;
    private int editAlignmentType = -1;
    private String editAlignmentUri;
    private String editAlignmentSource;

    public boolean isManagerActionsAvailable() {
        return userSession.isLoggedIn() && userSession.isManager();
    }

    public void prepareManualAlignment() {
        alignmentTypes = conceptAlignmentMutationService.listAlignmentTypes();
        manualAlignmentType = alignmentTypes.isEmpty() ? -1 : alignmentTypes.get(0).getId();
        manualAlignmentUri = "";
        manualAlignmentSource = "";
        candidatAlignmentSupport.resetManualAlignment();
    }

    public void prepareAutoAlignment() {
        if (!conceptSelectionContext.hasSelection()) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        candidatAlignmentSupport.prepareAutoAlignment(
                summary.preferredLabel(),
                summary.conceptId(),
                thesaurusContext.resolveThesaurusId(),
                thesaurusContext.resolveWorkLanguage()
        );
    }

    public boolean isHasAlignmentSources() {
        return candidatAlignmentSupport.hasAlignmentSources();
    }

    public void addAutoAlignment() {
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || !conceptSelectionContext.hasSelection()) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        candidatAlignmentSupport.addAlignment(
                thesaurusContext.resolveThesaurusId(),
                summary.conceptId(),
                userId
        );
        refreshConcept();
        MessageUtils.showInformationMessage("Alignement ajouté avec succès");
    }

    public void addManualAlignment() {
        if (StringUtils.isBlank(manualAlignmentUri)) {
            MessageUtils.showErrorMessage("Veuillez saisir une URI");
            return;
        }
        if (!fr.cnrs.opentheso.utils.StringUtils.urlValidator(manualAlignmentUri)) {
            MessageUtils.showErrorMessage("L'URI n'est pas valide");
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || !conceptSelectionContext.hasSelection()) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        if (!conceptAlignmentMutationService.addManualAlignment(
                userId,
                manualAlignmentSource,
                manualAlignmentUri,
                manualAlignmentType,
                summary.conceptId(),
                thesaurusContext.resolveThesaurusId()
        )) {
            MessageUtils.showErrorMessage("Erreur lors de l'ajout de l'alignement");
            return;
        }
        refreshConcept();
        PrimeFaces.current().executeScript("PF('v2AddManualAlignment').hide();");
        MessageUtils.showInformationMessage("Alignement ajouté avec succès");
    }

    public void prepareDelete(ConceptAlignment alignment) {
        alignmentToDelete = alignment;
    }

    public void deleteAlignment() {
        if (alignmentToDelete == null || StringUtils.isBlank(alignmentToDelete.id())) {
            return;
        }
        try {
            int alignmentId = Integer.parseInt(alignmentToDelete.id());
            if (!conceptAlignmentMutationService.deleteAlignment(alignmentId, thesaurusContext.resolveThesaurusId())) {
                MessageUtils.showErrorMessage("Erreur de suppression");
                return;
            }
            refreshConcept();
            MessageUtils.showInformationMessage("Alignement supprimé avec succès");
        } catch (NumberFormatException ex) {
            MessageUtils.showErrorMessage("Identifiant d'alignement invalide");
        }
    }

    public void prepareEdit(ConceptAlignment alignment) {
        alignmentToEdit = alignment;
        alignmentTypes = conceptAlignmentMutationService.listAlignmentTypes();
        editAlignmentUri = alignment.uri();
        editAlignmentSource = alignment.sourceName();
        editAlignmentType = alignmentTypes.isEmpty() ? -1 : alignmentTypes.get(0).getId();
        if (alignment.typeLabel() != null) {
            for (NodeAlignmentType type : alignmentTypes) {
                if (alignment.typeLabel().equalsIgnoreCase(type.getLabelSkos())
                        || alignment.typeLabel().equalsIgnoreCase(type.getLabel())) {
                    editAlignmentType = type.getId();
                    break;
                }
            }
        }
    }

    public void updateAlignment() {
        if (alignmentToEdit == null || StringUtils.isBlank(alignmentToEdit.id())) {
            return;
        }
        if (!conceptSelectionContext.hasSelection()) {
            return;
        }
        var summary = conceptSelectionContext.getSummary();
        var element = AlignementElement.builder()
                .idAlignment(Integer.parseInt(alignmentToEdit.id()))
                .alignement_id_type(editAlignmentType)
                .conceptTarget("")
                .thesaurus_target(editAlignmentSource)
                .targetUri(editAlignmentUri)
                .build();
        conceptAlignmentMutationService.updateAlignment(
                element,
                summary.conceptId(),
                thesaurusContext.resolveThesaurusId()
        );
        refreshConcept();
        PrimeFaces.current().executeScript("PF('v2EditAlignment').hide();");
        MessageUtils.showInformationMessage("Alignement mis à jour avec succès");
    }

    private void refreshConcept() {
        conceptNavigationSupport.refreshSelectedConcept();
        PrimeFaces.current().ajax().update("conceptSummaryPanel");
    }
}
