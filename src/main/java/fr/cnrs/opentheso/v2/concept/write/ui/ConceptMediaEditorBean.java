package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptExternalResourceItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.session.ConceptNavigationSupport;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusBrowseBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.policy.ConceptWritePolicy;
import fr.cnrs.opentheso.v2.concept.write.service.ConceptMediaMutationService;
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
import java.util.stream.Collectors;

@Getter
@Setter
@ViewScoped
@Named("v2ConceptMediaEditorBean")
@RequiredArgsConstructor
public class ConceptMediaEditorBean implements Serializable {

    private final ConceptMediaMutationService conceptMediaMutationService;
    private final ConceptSelectionContext conceptSelectionContext;
    private final ConceptNavigationSupport conceptNavigationSupport;
    private final ThesaurusContext thesaurusContext;
    private final UserSession userSession;
    private final ConceptWritePolicy conceptWritePolicy;
    private final ThesaurusBrowseBean thesaurusBrowseBean;

    private String currentConceptLabel;
    private String gpsCoordinatesText;
    private String imageUri;
    private String imageName;
    private String imageCreator;
    private String imageCopyright;
    private String resourceUri;
    private String resourceDescription;
    private List<ImageEditRow> imageEdits = Collections.emptyList();
    private List<ImageEditRow> imagesToDelete = Collections.emptyList();
    private List<ExternalResourceEditRow> resourceEdits = Collections.emptyList();
    private List<ExternalResourceEditRow> resourcesToDelete = Collections.emptyList();

    public boolean isMediaActionsAvailable() {
        return conceptWritePolicy.canMutateMedia(userSession, isSelectedDeprecated());
    }

    public void prepareEditGps() {
        refreshCurrentConceptLabel();
        gpsCoordinatesText = formatGpsCoordinates(loadGpsPoints());
    }

    public void prepareAddImage() {
        resetImageForm();
        refreshCurrentConceptLabel();
    }

    public void prepareEditImages() {
        refreshCurrentConceptLabel();
        imageEdits = loadImageEdits();
    }

    public void prepareDeleteImages() {
        refreshCurrentConceptLabel();
        imagesToDelete = loadImageEdits();
    }

    public void prepareAddExternalResource() {
        resetResourceForm();
        refreshCurrentConceptLabel();
    }

    public void prepareEditExternalResources() {
        refreshCurrentConceptLabel();
        resourceEdits = loadResourceEdits();
    }

    public void prepareDeleteExternalResources() {
        refreshCurrentConceptLabel();
        resourcesToDelete = loadResourceEdits();
    }

    public void submitSaveGps() {
        Integer userId = requireUserId();
        if (userId == null || !isMediaActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new ReplaceGpsCoordinatesCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                gpsCoordinatesText
        );
        handleMutationResult(conceptMediaMutationService.replaceGpsCoordinates(command), "v2EditGpsDlg");
    }

    public void submitAddImage() {
        Integer userId = requireUserId();
        if (userId == null || !isMediaActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new AddConceptImageCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                imageUri,
                imageName,
                imageCreator,
                imageCopyright
        );
        if (handleMutationResult(conceptMediaMutationService.addImage(command), "v2AddImageDlg")) {
            resetImageForm();
        }
    }

    public void submitUpdateImage(ImageEditRow row) {
        Integer userId = requireUserId();
        if (userId == null || row == null || !isMediaActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new UpdateConceptImageCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                row.getId(),
                row.getUri(),
                row.getName(),
                row.getCreator(),
                row.getCopyright()
        );
        handleMutationResult(conceptMediaMutationService.updateImage(command), null);
        imageEdits = loadImageEdits();
    }

    public void submitDeleteImage(ImageEditRow row) {
        Integer userId = requireUserId();
        if (userId == null || row == null || !isMediaActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new DeleteConceptImageCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                row.getUri()
        );
        if (!handleMutationResult(conceptMediaMutationService.deleteImage(command), null)) {
            return;
        }
        // Retirer la ligne du dialogue immédiatement (comme legacy reset() après delete)
        int deletedId = row.getId();
        String deletedUri = row.getUri();
        imagesToDelete = imagesToDelete.stream()
                .filter(image -> deletedId > 0
                        ? image.getId() != deletedId
                        : !StringUtils.equals(image.getUri(), deletedUri))
                .collect(Collectors.toList());
        PrimeFaces.current().ajax().update(":containerIndex:v2DeleteImageDlg");
    }

    public void submitAddExternalResource() {
        Integer userId = requireUserId();
        if (userId == null || !isMediaActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new AddExternalResourceCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                resourceUri,
                resourceDescription
        );
        if (handleMutationResult(conceptMediaMutationService.addExternalResource(command), "v2AddExternalResourceDlg")) {
            resetResourceForm();
        }
    }

    public void submitUpdateExternalResource(ExternalResourceEditRow row) {
        Integer userId = requireUserId();
        if (userId == null || row == null || !isMediaActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new UpdateExternalResourceCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                row.getOldUri(),
                row.getUri(),
                row.getDescription()
        );
        handleMutationResult(conceptMediaMutationService.updateExternalResource(command), null);
        resourceEdits = loadResourceEdits();
    }

    public void submitDeleteExternalResource(ExternalResourceEditRow row) {
        Integer userId = requireUserId();
        if (userId == null || row == null || !isMediaActionsAvailable() || !conceptSelectionContext.hasSelection()) {
            MessageUtils.showErrorMessage("Action non autorisée");
            return;
        }
        var command = new DeleteExternalResourceCommand(
                thesaurusContext.resolveThesaurusId(),
                conceptSelectionContext.getConceptId(),
                userId,
                contributorName(),
                row.getUri()
        );
        if (!handleMutationResult(conceptMediaMutationService.deleteExternalResource(command), null)) {
            return;
        }
        String deletedUri = row.getUri();
        resourcesToDelete = resourcesToDelete.stream()
                .filter(resource -> !StringUtils.equals(resource.getUri(), deletedUri)
                        && !StringUtils.equals(resource.getOldUri(), deletedUri))
                .collect(Collectors.toList());
        PrimeFaces.current().ajax().update(":containerIndex:v2DeleteExternalResourceDlg");
    }

    private boolean handleMutationResult(MutationResult result, String dialogWidget) {
        if (result == null || !result.success()) {
            MessageUtils.showErrorMessage(result != null ? result.message() : "Erreur");
            return false;
        }
        conceptNavigationSupport.refreshSelectedConcept();
        PrimeFaces.current().ajax().update(":containerIndex:formRightTab :messageIndex");
        MessageUtils.showInformationMessage(result.message());
        if (StringUtils.isNotBlank(dialogWidget)) {
            PrimeFaces.current().executeScript("PF('" + dialogWidget + "').hide();");
        }
        return true;
    }

    private List<ConceptGpsPoint> loadGpsPoints() {
        if (thesaurusBrowseBean.getSelectedConcept() == null) {
            return Collections.emptyList();
        }
        return thesaurusBrowseBean.getSelectedConcept().gpsPoints() != null
                ? thesaurusBrowseBean.getSelectedConcept().gpsPoints()
                : Collections.emptyList();
    }

    private List<ImageEditRow> loadImageEdits() {
        if (thesaurusBrowseBean.getSelectedConcept() == null
                || thesaurusBrowseBean.getSelectedConcept().images() == null) {
            return Collections.emptyList();
        }
        return thesaurusBrowseBean.getSelectedConcept().images().stream()
                .map(this::toImageEditRow)
                .collect(Collectors.toList());
    }

    private List<ExternalResourceEditRow> loadResourceEdits() {
        if (thesaurusBrowseBean.getSelectedConcept() == null
                || thesaurusBrowseBean.getSelectedConcept().externalResources() == null) {
            return Collections.emptyList();
        }
        return thesaurusBrowseBean.getSelectedConcept().externalResources().stream()
                .map(this::toResourceEditRow)
                .collect(Collectors.toList());
    }

    private ImageEditRow toImageEditRow(ConceptImageItem image) {
        return new ImageEditRow(
                image.id(),
                image.uri(),
                image.imageName(),
                image.creator(),
                image.copyright()
        );
    }

    private ExternalResourceEditRow toResourceEditRow(ConceptExternalResourceItem resource) {
        return new ExternalResourceEditRow(resource.uri(), resource.description());
    }

    private String formatGpsCoordinates(List<ConceptGpsPoint> points) {
        if (points == null || points.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (ConceptGpsPoint point : points) {
            result.append(String.format("%f %f", point.latitude(), point.longitude()).replace(",", "."))
                    .append(", ");
        }
        return "(" + result.substring(0, result.length() - 2) + ")";
    }

    private void resetImageForm() {
        imageUri = "";
        imageName = "";
        imageCreator = "";
        imageCopyright = "";
    }

    private void resetResourceForm() {
        resourceUri = "";
        resourceDescription = "";
    }

    private void refreshCurrentConceptLabel() {
        currentConceptLabel = conceptSelectionContext.hasSelection()
                ? conceptSelectionContext.getSummary().preferredLabel()
                : "";
    }

    private Integer requireUserId() {
        return userSession.getCurrentUserId();
    }

    private String contributorName() {
        return StringUtils.defaultString(userSession.getCurrentUsername());
    }

    private boolean isSelectedDeprecated() {
        if (!conceptSelectionContext.hasSelection()) {
            return false;
        }
        return "dep".equalsIgnoreCase(StringUtils.trimToEmpty(conceptSelectionContext.getSummary().status()));
    }
}
