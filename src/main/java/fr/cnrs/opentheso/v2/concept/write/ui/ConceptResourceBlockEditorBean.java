package fr.cnrs.opentheso.v2.concept.write.ui;

import fr.cnrs.opentheso.utils.StringUtils;
import fr.cnrs.opentheso.v2.concept.model.ConceptDetail;
import fr.cnrs.opentheso.v2.concept.model.ConceptExternalResourceItem;
import fr.cnrs.opentheso.v2.concept.model.ConceptGpsPoint;
import fr.cnrs.opentheso.v2.concept.model.ConceptImageItem;
import fr.cnrs.opentheso.v2.concept.session.ConceptSelectionContext;
import fr.cnrs.opentheso.v2.concept.ui.ThesaurusViewBean;
import fr.cnrs.opentheso.v2.concept.write.model.MutationOutcome;
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
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Édition inline par champ du bloc Ressources externes (liens, images, GPS).
 */
@Getter
@Setter
@ViewScoped
@Named("v2ConceptResourceBlockEditorBean")
@RequiredArgsConstructor
public class ConceptResourceBlockEditorBean implements Serializable {

    static final String CARD_LINKS = "resLiens";
    static final String CARD_IMAGES = "resImages";
    static final String CARD_GPS = "resGps";

    private final transient ThesaurusViewBean thesaurusViewBean;
    private final transient ConceptMediaMutationService conceptMediaMutationService;
    private final transient ConceptWritePolicy conceptWritePolicy;
    private final transient UserSession userSession;
    private final transient ConceptSelectionContext conceptSelectionContext;

    @Getter(AccessLevel.NONE)
    private boolean editing;
    private String editingConceptId;
    private String editingSection;
    private List<ExternalResourceEditRow> resourceRows = new ArrayList<>();
    private List<ImageEditRow> imageRows = new ArrayList<>();
    private List<GpsEditRow> gpsRows = new ArrayList<>();
    private String clickLatitude;
    private String clickLongitude;
    private String errorMessage;
    private String flashMessage;
    private String flashToken;

    public boolean isEditable() {
        return thesaurusViewBean.getSelectedConcept() != null
                && conceptWritePolicy.canMutateMedia(
                        userSession, thesaurusViewBean.isSelectedConceptDeprecated());
    }

    public boolean isEditing() {
        if (editing && !matchesCurrentConcept()) {
            resetForm(false);
        }
        return editing && isResourceCard(thesaurusViewBean.getFicheEditCard());
    }

    public boolean isEditingLinks() {
        return isEditing() && CARD_LINKS.equals(thesaurusViewBean.getFicheEditCard());
    }

    public boolean isEditingImages() {
        return isEditing() && CARD_IMAGES.equals(thesaurusViewBean.getFicheEditCard());
    }

    public boolean isEditingGps() {
        return isEditing() && CARD_GPS.equals(thesaurusViewBean.getFicheEditCard());
    }

    public boolean isCanClosePolygon() {
        if (!isEditingGps() || gpsRows.size() < 3) {
            return false;
        }
        GpsEditRow first = gpsRows.get(0);
        GpsEditRow last = gpsRows.get(gpsRows.size() - 1);
        return !samePoint(first, last);
    }

    public String getConfirmTitleKey() {
        if (isEditingImages()) {
            return "v2.concept.resource.images.confirmTitle";
        }
        if (isEditingGps()) {
            return "v2.concept.resource.gps.confirmTitle";
        }
        return "v2.concept.resource.links.confirmTitle";
    }

    public String getConfirmMessageKey() {
        if (isEditingImages()) {
            return "v2.concept.resource.images.confirm";
        }
        if (isEditingGps()) {
            return "v2.concept.resource.gps.confirm";
        }
        return "v2.concept.resource.links.confirm";
    }

    public void startEditingLinks() {
        beginEditing(CARD_LINKS);
    }

    public void startEditingImages() {
        beginEditing(CARD_IMAGES);
    }

    public void startEditingGps() {
        beginEditing(CARD_GPS);
    }

    public void cancel() {
        resetForm(false);
    }

    public void addResourceRow() {
        if (!isEditingLinks()) {
            return;
        }
        resourceRows.add(new ExternalResourceEditRow("", ""));
    }

    public void removeResourceRow(int index) {
        if (!isEditingLinks() || index < 0 || index >= resourceRows.size()) {
            return;
        }
        resourceRows.remove(index);
    }

    public void addImageRow() {
        if (!isEditingImages()) {
            return;
        }
        imageRows.add(new ImageEditRow(0, "", "", "", ""));
    }

    public void removeImageRow(int index) {
        if (!isEditingImages() || index < 0 || index >= imageRows.size()) {
            return;
        }
        imageRows.remove(index);
    }

    public void addGpsRow() {
        if (!isEditingGps()) {
            return;
        }
        gpsRows.add(new GpsEditRow());
    }

    public void addGpsAtClick() {
        if (!isEditingGps()) {
            return;
        }
        String lat = normalizeCoord(clickLatitude);
        String lng = normalizeCoord(clickLongitude);
        if (!isCoord(lat) || !isCoord(lng)) {
            return;
        }
        gpsRows.add(new GpsEditRow(lat, lng));
        clickLatitude = "";
        clickLongitude = "";
    }

    public void removeGpsRow(int index) {
        if (!isEditingGps() || index < 0 || index >= gpsRows.size()) {
            return;
        }
        gpsRows.remove(index);
    }

    public void closePolygon() {
        if (!isCanClosePolygon()) {
            return;
        }
        GpsEditRow first = gpsRows.get(0);
        gpsRows.add(new GpsEditRow(
                org.apache.commons.lang3.StringUtils.trimToEmpty(first.getLatitude()),
                org.apache.commons.lang3.StringUtils.trimToEmpty(first.getLongitude())));
    }

    public void save() {
        errorMessage = "";
        if (!isEditable() || !isEditing()) {
            return;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        ConceptDetail current = thesaurusViewBean.getSelectedConcept();
        if (current == null || current.getSummary() == null) {
            errorMessage = WriteUiMessages.UNAUTHORIZED_FALLBACK;
            return;
        }
        String thesaurusId = thesaurusViewBean.getId();
        String conceptId = current.getSummary().getConceptId();
        String contributor = org.apache.commons.lang3.StringUtils.defaultString(userSession.getCurrentUsername());
        if (isEditingGps()) {
            if (!persistGps(current, thesaurusId, conceptId, userId, contributor)) {
                return;
            }
        } else if (isEditingImages()) {
            if (!persistImages(current, thesaurusId, conceptId, userId, contributor)) {
                return;
            }
        } else if (isEditingLinks()) {
            if (!persistLinks(current, thesaurusId, conceptId, userId, contributor)) {
                return;
            }
        } else {
            return;
        }
        finishSuccess();
    }

    private void beginEditing(String card) {
        if (!isEditable()) {
            return;
        }
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return;
        }
        editingConceptId = detail.getSummary().getConceptId();
        editingSection = card;
        resourceRows = CARD_LINKS.equals(card) ? copyResources(detail) : new ArrayList<>();
        imageRows = CARD_IMAGES.equals(card) ? copyImages(detail) : new ArrayList<>();
        gpsRows = CARD_GPS.equals(card) ? copyGps(detail) : new ArrayList<>();
        clickLatitude = "";
        clickLongitude = "";
        errorMessage = "";
        flashMessage = "";
        flashToken = "";
        editing = true;
        thesaurusViewBean.setFicheEditCard(card);
        conceptSelectionContext.update(thesaurusViewBean.getId(), detail);
    }

    private boolean persistGps(
            ConceptDetail current,
            String thesaurusId,
            String conceptId,
            int userId,
            String contributor
    ) {
        String gpsText = serializeGps();
        if (gpsText == null) {
            return false;
        }
        if (org.apache.commons.lang3.StringUtils.equals(gpsText, formatGps(current.getGpsPoints()))) {
            return true;
        }
        return applyResult(conceptMediaMutationService.replaceGpsCoordinates(
                new ReplaceGpsCoordinatesCommand(thesaurusId, conceptId, userId, contributor, gpsText)), false);
    }

    private boolean persistImages(
            ConceptDetail current,
            String thesaurusId,
            String conceptId,
            int userId,
            String contributor
    ) {
        List<ImageEditRow> images = normalizedImages();
        if (images == null) {
            return false;
        }
        boolean dirty = false;
        Set<Integer> keptImageIds = new LinkedHashSet<>();
        for (ImageEditRow row : images) {
            if (row.getId() > 0) {
                keptImageIds.add(row.getId());
            }
        }
        for (ConceptImageItem old : safeImages(current)) {
            if (old.id() > 0 && !keptImageIds.contains(old.id())) {
                MutationResult deleted = conceptMediaMutationService.deleteImage(new DeleteConceptImageCommand(
                        thesaurusId, conceptId, userId, contributor, old.uri()));
                if (!applyResult(deleted, dirty)) {
                    return false;
                }
                dirty = true;
            }
        }
        for (ImageEditRow row : images) {
            if (row.getId() > 0) {
                ConceptImageItem previous = imageById(current, row.getId());
                if (previous != null && imageUnchanged(previous, row)) {
                    continue;
                }
                MutationResult updated = conceptMediaMutationService.updateImage(new UpdateConceptImageCommand(
                        thesaurusId, conceptId, userId, contributor,
                        row.getId(), row.getUri(), row.getName(), row.getCreator(), row.getCopyright()));
                if (!applyResult(updated, dirty)) {
                    return false;
                }
                dirty = true;
            } else {
                MutationResult added = conceptMediaMutationService.addImage(new AddConceptImageCommand(
                        thesaurusId, conceptId, userId, contributor,
                        row.getUri(), row.getName(), row.getCreator(), row.getCopyright()));
                if (!applyResult(added, dirty)) {
                    return false;
                }
                dirty = true;
            }
        }
        return true;
    }

    private boolean persistLinks(
            ConceptDetail current,
            String thesaurusId,
            String conceptId,
            int userId,
            String contributor
    ) {
        List<ExternalResourceEditRow> resources = normalizedResources();
        if (resources == null) {
            return false;
        }
        boolean dirty = false;
        Set<String> keptResourceUris = new LinkedHashSet<>();
        for (ExternalResourceEditRow row : resources) {
            String key = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getOldUri());
            if (!key.isEmpty()) {
                keptResourceUris.add(key);
            }
        }
        for (ConceptExternalResourceItem old : safeResources(current)) {
            String uri = org.apache.commons.lang3.StringUtils.trimToEmpty(old.uri());
            if (!uri.isEmpty() && !keptResourceUris.contains(uri)) {
                MutationResult deleted = conceptMediaMutationService.deleteExternalResource(
                        new DeleteExternalResourceCommand(thesaurusId, conceptId, userId, contributor, uri));
                if (!applyResult(deleted, dirty)) {
                    return false;
                }
                dirty = true;
            }
        }
        for (ExternalResourceEditRow row : resources) {
            String oldUri = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getOldUri());
            if (oldUri.isEmpty()) {
                MutationResult added = conceptMediaMutationService.addExternalResource(new AddExternalResourceCommand(
                        thesaurusId, conceptId, userId, contributor, row.getUri(), row.getDescription()));
                if (!applyResult(added, dirty)) {
                    return false;
                }
                dirty = true;
            } else {
                ConceptExternalResourceItem previous = resourceByUri(current, oldUri);
                if (previous != null && resourceUnchanged(previous, row)) {
                    continue;
                }
                MutationResult updated = conceptMediaMutationService.updateExternalResource(
                        new UpdateExternalResourceCommand(
                                thesaurusId, conceptId, userId, contributor,
                                oldUri, row.getUri(), row.getDescription()));
                if (!applyResult(updated, dirty)) {
                    return false;
                }
                dirty = true;
            }
        }
        return true;
    }

    private List<ExternalResourceEditRow> normalizedResources() {
        List<ExternalResourceEditRow> cleaned = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ExternalResourceEditRow row : resourceRows) {
            if (row == null) {
                continue;
            }
            String uri = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getUri());
            String description = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getDescription());
            String oldUri = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getOldUri());
            if (uri.isEmpty() && description.isEmpty() && oldUri.isEmpty()) {
                continue;
            }
            if (uri.isEmpty()) {
                errorMessage = "L'URI de la ressource est obligatoire.";
                return null;
            }
            if (!StringUtils.urlValidator(uri)) {
                errorMessage = "L'URL n'est pas valide !";
                return null;
            }
            if (!seen.add(uri)) {
                errorMessage = "Chaque URI de ressource doit être unique.";
                return null;
            }
            ExternalResourceEditRow copy = new ExternalResourceEditRow(oldUri, description);
            copy.setOldUri(oldUri);
            copy.setUri(uri);
            copy.setDescription(description);
            cleaned.add(copy);
        }
        return cleaned;
    }

    private List<ImageEditRow> normalizedImages() {
        List<ImageEditRow> cleaned = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ImageEditRow row : imageRows) {
            if (row == null) {
                continue;
            }
            String uri = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getUri());
            String name = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getName());
            String creator = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getCreator());
            String copyright = org.apache.commons.lang3.StringUtils.trimToEmpty(row.getCopyright());
            if (uri.isEmpty() && name.isEmpty() && creator.isEmpty() && copyright.isEmpty() && row.getId() <= 0) {
                continue;
            }
            if (uri.isEmpty()) {
                errorMessage = "L'URI de l'image est obligatoire.";
                return null;
            }
            if (!seen.add(uri)) {
                errorMessage = "Chaque URI d'image doit être unique.";
                return null;
            }
            ImageEditRow copy = new ImageEditRow(row.getId(), uri, name, creator, copyright);
            copy.setOldUri(org.apache.commons.lang3.StringUtils.trimToEmpty(row.getOldUri()));
            cleaned.add(copy);
        }
        return cleaned;
    }

    private String serializeGps() {
        List<String> pairs = new ArrayList<>();
        for (GpsEditRow row : gpsRows) {
            if (row == null) {
                continue;
            }
            String pair = formatGpsPair(row);
            if (pair == null) {
                return null;
            }
            if (!pair.isEmpty()) {
                pairs.add(pair);
            }
        }
        if (pairs.isEmpty()) {
            return "";
        }
        return "(" + String.join(", ", pairs) + ")";
    }

    private String formatGpsPair(GpsEditRow row) {
        String lat = normalizeCoord(row.getLatitude());
        String lng = normalizeCoord(row.getLongitude());
        if (lat.isEmpty() && lng.isEmpty()) {
            return "";
        }
        if (lat.isEmpty() || lng.isEmpty()) {
            errorMessage = "Chaque point GPS doit avoir une latitude et une longitude.";
            return null;
        }
        if (!isCoord(lat) || !isCoord(lng)) {
            errorMessage = "Coordonnées GPS invalides.";
            return null;
        }
        double latitude = Double.parseDouble(lat);
        double longitude = Double.parseDouble(lng);
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            errorMessage = "Coordonnées GPS hors limites.";
            return null;
        }
        return lat + " " + lng;
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
        errorMessage = org.apache.commons.lang3.StringUtils.defaultIfBlank(result.message(), "L'enregistrement a échoué.");
        reloadIfDirty(dirty);
        return false;
    }

    private void finishSuccess() {
        String section = thesaurusViewBean.getFicheEditCard();
        editing = false;
        if (isResourceCard(section)) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        errorMessage = "";
        if (CARD_IMAGES.equals(section)) {
            flashMessage = "Images enregistrées";
        } else if (CARD_GPS.equals(section)) {
            flashMessage = "Coordonnées GPS enregistrées";
        } else {
            flashMessage = "Liens enregistrés";
        }
        flashToken = String.valueOf(System.currentTimeMillis());
        thesaurusViewBean.reloadSelectedConcept();
        conceptSelectionContext.update(thesaurusViewBean.getId(), thesaurusViewBean.getSelectedConcept());
    }

    private void reloadIfDirty(boolean dirty) {
        if (dirty) {
            thesaurusViewBean.reloadSelectedConcept();
        }
    }

    private void resetForm(boolean keepFlash) {
        editing = false;
        if (isResourceCard(thesaurusViewBean.getFicheEditCard())) {
            thesaurusViewBean.setFicheEditCard(null);
        }
        editingConceptId = null;
        editingSection = null;
        resourceRows = new ArrayList<>();
        imageRows = new ArrayList<>();
        gpsRows = new ArrayList<>();
        clickLatitude = "";
        clickLongitude = "";
        errorMessage = "";
        if (!keepFlash) {
            flashMessage = "";
            flashToken = "";
        }
    }

    private static boolean isResourceCard(String card) {
        return CARD_LINKS.equals(card) || CARD_IMAGES.equals(card) || CARD_GPS.equals(card);
    }

    private boolean matchesCurrentConcept() {
        ConceptDetail detail = thesaurusViewBean.getSelectedConcept();
        if (detail == null || detail.getSummary() == null) {
            return false;
        }
        return org.apache.commons.lang3.StringUtils.equals(editingConceptId, detail.getSummary().getConceptId());
    }

    private static List<ExternalResourceEditRow> copyResources(ConceptDetail detail) {
        List<ExternalResourceEditRow> copied = new ArrayList<>();
        for (ConceptExternalResourceItem item : safeResources(detail)) {
            copied.add(new ExternalResourceEditRow(
                    org.apache.commons.lang3.StringUtils.defaultString(item.uri()),
                    org.apache.commons.lang3.StringUtils.defaultString(item.description())));
        }
        return copied;
    }

    private static List<ImageEditRow> copyImages(ConceptDetail detail) {
        List<ImageEditRow> copied = new ArrayList<>();
        for (ConceptImageItem image : safeImages(detail)) {
            copied.add(new ImageEditRow(
                    image.id(),
                    org.apache.commons.lang3.StringUtils.defaultString(image.uri()),
                    org.apache.commons.lang3.StringUtils.defaultString(image.imageName()),
                    org.apache.commons.lang3.StringUtils.defaultString(image.creator()),
                    org.apache.commons.lang3.StringUtils.defaultString(image.copyright())));
        }
        return copied;
    }

    private static List<GpsEditRow> copyGps(ConceptDetail detail) {
        List<GpsEditRow> copied = new ArrayList<>();
        if (detail == null || detail.getGpsPoints() == null) {
            return copied;
        }
        for (ConceptGpsPoint point : detail.getGpsPoints()) {
            copied.add(new GpsEditRow(point.latText(), point.lngText()));
        }
        return copied;
    }

    private static List<ConceptImageItem> safeImages(ConceptDetail detail) {
        if (detail == null || detail.getImages() == null) {
            return List.of();
        }
        return detail.getImages();
    }

    private static List<ConceptExternalResourceItem> safeResources(ConceptDetail detail) {
        if (detail == null || detail.getExternalResources() == null) {
            return List.of();
        }
        return detail.getExternalResources();
    }

    private static ConceptImageItem imageById(ConceptDetail detail, int id) {
        for (ConceptImageItem image : safeImages(detail)) {
            if (image.id() == id) {
                return image;
            }
        }
        return null;
    }

    private static ConceptExternalResourceItem resourceByUri(ConceptDetail detail, String uri) {
        for (ConceptExternalResourceItem item : safeResources(detail)) {
            if (org.apache.commons.lang3.StringUtils.equals(
                    org.apache.commons.lang3.StringUtils.trimToEmpty(item.uri()), uri)) {
                return item;
            }
        }
        return null;
    }

    private static boolean imageUnchanged(ConceptImageItem previous, ImageEditRow row) {
        return org.apache.commons.lang3.StringUtils.equals(
                org.apache.commons.lang3.StringUtils.trimToEmpty(previous.uri()), row.getUri())
                && org.apache.commons.lang3.StringUtils.equals(
                        org.apache.commons.lang3.StringUtils.trimToEmpty(previous.imageName()), row.getName())
                && org.apache.commons.lang3.StringUtils.equals(
                        org.apache.commons.lang3.StringUtils.trimToEmpty(previous.creator()), row.getCreator())
                && org.apache.commons.lang3.StringUtils.equals(
                        org.apache.commons.lang3.StringUtils.trimToEmpty(previous.copyright()), row.getCopyright());
    }

    private static boolean resourceUnchanged(ConceptExternalResourceItem previous, ExternalResourceEditRow row) {
        return org.apache.commons.lang3.StringUtils.equals(
                org.apache.commons.lang3.StringUtils.trimToEmpty(previous.uri()), row.getUri())
                && org.apache.commons.lang3.StringUtils.equals(
                        org.apache.commons.lang3.StringUtils.trimToEmpty(previous.description()),
                        org.apache.commons.lang3.StringUtils.trimToEmpty(row.getDescription()));
    }

    private static boolean samePoint(GpsEditRow left, GpsEditRow right) {
        return org.apache.commons.lang3.StringUtils.equals(
                normalizeCoord(left.getLatitude()), normalizeCoord(right.getLatitude()))
                && org.apache.commons.lang3.StringUtils.equals(
                        normalizeCoord(left.getLongitude()), normalizeCoord(right.getLongitude()));
    }

    static String formatGps(List<ConceptGpsPoint> points) {
        if (points == null || points.isEmpty()) {
            return "";
        }
        List<String> pairs = new ArrayList<>();
        for (ConceptGpsPoint point : points) {
            pairs.add(point.latText() + " " + point.lngText());
        }
        return "(" + String.join(", ", pairs) + ")";
    }

    static String normalizeCoord(String raw) {
        String text = org.apache.commons.lang3.StringUtils.trimToEmpty(raw).replace(",", ".");
        if (text.matches("-?[0-9]+")) {
            return text + ".0";
        }
        return text;
    }

    private static boolean isCoord(String value) {
        return value.matches("-?[0-9]+\\.[0-9]+");
    }
}
