package fr.cnrs.opentheso.concept.write.persistence;

import fr.cnrs.opentheso.concept.history.ConceptChangeLog;
import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.utils.StringUtils;
import fr.cnrs.opentheso.concept.write.model.MutationResult;
import fr.cnrs.opentheso.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.concept.write.model.command.UpdateExternalResourceCommand;
import fr.cnrs.opentheso.concept.write.support.ConceptGpsCoordinateParser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConceptMediaWritePersistence {

    private static final String NO_IMAGE_SELECTED = "Aucune image sélectionnée !";

    private final GpsRepository gpsRepository;
    private final ImagesRepository imagesRepository;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;
    private final ConceptChangeLog changeLog;

    public MutationResult replaceGpsCoordinates(ReplaceGpsCoordinatesCommand command) {
        String before = gpsText(gpsRepository.findByIdConceptAndIdThesoOrderByPosition(
                command.conceptId(), command.thesaurusId()));
        if (org.apache.commons.lang3.StringUtils.isBlank(command.coordinatesText())) {
            if (!before.isEmpty()) {
                logGps(command, "DELETE", before, null);
            }
            gpsRepository.deleteByIdConceptAndIdTheso(command.conceptId(), command.thesaurusId());
            touchConcept(command);
            return MutationResult.ok("Coordonnée GPS modifiés !");
        }

        List<Gps> parsed = ConceptGpsCoordinateParser.parse(
                command.coordinatesText(),
                command.thesaurusId(),
                command.conceptId()
        );
        if (CollectionUtils.isEmpty(parsed)) {
            return MutationResult.validationError("Aucune coordonnée GPS trouvée !");
        }

        gpsRepository.deleteByIdConceptAndIdTheso(command.conceptId(), command.thesaurusId());
        for (Gps gps : parsed) {
            gpsRepository.save(gps);
        }
        String after = gpsText(parsed);
        if (!after.equals(before)) {
            logGps(command, before.isEmpty() ? "ADD" : "UPDATE", before.isEmpty() ? null : before, after);
        }
        touchConcept(command);
        return MutationResult.ok("Coordonnée GPS modifiés !");
    }

    public MutationResult addImage(AddConceptImageCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Aucune URI insérée !");
        }
        imagesRepository.save(ImageExterne.builder()
                .imageCreator(command.creator())
                .idUser(command.userId())
                .idConcept(command.conceptId())
                .idThesaurus(command.thesaurusId())
                .imageName(org.apache.commons.lang3.StringUtils.defaultString(command.name()))
                .imageCopyright(command.copyright())
                .externalUri(command.uri().trim())
                .build());
        logMedia(command.thesaurusId(), command.conceptId(), ConceptChangeLog.IMAGE, command.uri().trim(),
                "ADD", null, command.uri().trim(), command.userId());
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Image ajoutée avec succès");
    }

    public MutationResult updateImage(UpdateConceptImageCommand command) {
        if (command.imageId() <= 0) {
            return MutationResult.validationError(NO_IMAGE_SELECTED);
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError(NO_IMAGE_SELECTED);
        }
        var existing = imagesRepository.findById(command.imageId()).orElse(null);
        if (existing == null
                || !org.apache.commons.lang3.Strings.CS.equals(existing.getIdConcept(), command.conceptId())
                || !org.apache.commons.lang3.Strings.CS.equals(existing.getIdThesaurus(), command.thesaurusId())) {
            return MutationResult.validationError(NO_IMAGE_SELECTED);
        }
        if (!org.apache.commons.lang3.Strings.CS.equals(existing.getExternalUri(), command.uri())) {
            logMedia(command.thesaurusId(), command.conceptId(), ConceptChangeLog.IMAGE, String.valueOf(command.imageId()),
                    "UPDATE", existing.getExternalUri(), command.uri(), command.userId());
        }
        existing.setImageCreator(command.creator());
        existing.setExternalUri(command.uri());
        existing.setImageName(command.name());
        existing.setImageCopyright(command.copyright());
        imagesRepository.save(existing);
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("L'URI du l'image est modifiée avec succès");
    }

    public MutationResult deleteImage(DeleteConceptImageCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        logMedia(command.thesaurusId(), command.conceptId(), ConceptChangeLog.IMAGE, command.uri(),
                "DELETE", command.uri(), null, command.userId());
        imagesRepository.deleteByIdThesaurusAndIdConceptAndExternalUri(
                command.thesaurusId(), command.conceptId(), command.uri());
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Image supprimée avec succès");
    }

    public MutationResult addExternalResource(AddExternalResourceCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Pas de sélection !");
        }
        if (!StringUtils.urlValidator(command.uri())) {
            return MutationResult.validationError("L'URL n'est pas valide !");
        }
        externalResourcesRepository.save(ExternalResource.builder()
                .idConcept(command.conceptId())
                .idThesaurus(command.thesaurusId())
                .description(command.description())
                .externalUri(command.uri())
                .build());
        logMedia(command.thesaurusId(), command.conceptId(), ConceptChangeLog.RESOURCE, command.uri(),
                "ADD", null, command.uri(), command.userId());
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Ressource ajoutée avec succès");
    }

    public MutationResult updateExternalResource(UpdateExternalResourceCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Pas de sélection !");
        }
        if (!StringUtils.urlValidator(command.uri())) {
            return MutationResult.validationError("L'URL n'est pas valide !");
        }
        if (!org.apache.commons.lang3.Strings.CS.equals(command.oldUri(), command.uri())) {
            logMedia(command.thesaurusId(), command.conceptId(), ConceptChangeLog.RESOURCE, command.oldUri(),
                    "UPDATE", command.oldUri(), command.uri(), command.userId());
        }
        externalResourcesRepository.updateExternalResource(
                command.uri(),
                command.userId() + "",
                command.description(),
                command.conceptId(),
                command.thesaurusId(),
                command.oldUri()
        );
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Ressource modifiée avec succès");
    }

    public MutationResult deleteExternalResource(DeleteExternalResourceCommand command) {
        if (ObjectUtils.isEmpty(command.uri())) {
            return MutationResult.validationError("Aucune resource n'est sélectionnée !");
        }
        logMedia(command.thesaurusId(), command.conceptId(), ConceptChangeLog.RESOURCE, command.uri(),
                "DELETE", command.uri(), null, command.userId());
        externalResourcesRepository.deleteExternalResource(
                command.thesaurusId(),
                command.conceptId(),
                command.uri()
        );
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Ressource supprimée avec succès");
    }

    private void logGps(ReplaceGpsCoordinatesCommand command, String action, String oldValue, String newValue) {
        logMedia(command.thesaurusId(), command.conceptId(), ConceptChangeLog.GPS, null,
                action, oldValue, newValue, command.userId());
    }

    private void logMedia(String thesaurusId, String conceptId, String element, String itemKey,
                          String action, String oldValue, String newValue, int userId) {
        changeLog.record(ConceptChangeLog.Change.of(thesaurusId, conceptId, element, itemKey, null,
                action, oldValue, newValue, null, userId));
    }

    /** « lat, long » par point, séparés par « ; », dans l'ordre des positions. */
    static String gpsText(List<Gps> points) {
        if (points == null) {
            return "";
        }
        return points.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .map(p -> p.getLatitude() + ", " + p.getLongitude())
                .collect(java.util.stream.Collectors.joining(" ; "));
    }

    private void touchConcept(ReplaceGpsCoordinatesCommand command) {
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
    }

    private void touchConcept(String thesaurusId, String conceptId, int userId, String contributorName) {
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(contributorName)) {
            conceptWritePostMutationRepository.saveContributorDcTerm(thesaurusId, conceptId, contributorName);
        }
    }
}
