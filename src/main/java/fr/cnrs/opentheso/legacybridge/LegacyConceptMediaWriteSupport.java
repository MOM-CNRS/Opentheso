package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GpsService;
import fr.cnrs.opentheso.services.ImageService;
import fr.cnrs.opentheso.utils.StringUtils;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptMediaWritePort;
import fr.cnrs.opentheso.v2.concept.write.support.ConceptGpsCoordinateParser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyConceptMediaWriteSupport implements ConceptMediaWritePort {

    private final GpsService gpsService;
    private final ImageService imageService;
    private final ConceptService conceptService;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final ExternalResourcesRepository externalResourcesRepository;

    @Override
    public MutationResult replaceGpsCoordinates(ReplaceGpsCoordinatesCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.coordinatesText())) {
            gpsService.deleteGpsByConceptIdAndThesaurusId(command.conceptId(), command.thesaurusId());
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

        gpsService.deleteGpsByConceptIdAndThesaurusId(command.conceptId(), command.thesaurusId());
        for (Gps gps : parsed) {
            gpsService.saveNewGps(gps);
        }
        touchConcept(command);
        return MutationResult.ok("Coordonnée GPS modifiés !");
    }

    @Override
    public MutationResult addImage(AddConceptImageCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Aucune URI insérée !");
        }
        imageService.addExternalImage(
                command.conceptId(),
                command.thesaurusId(),
                command.name(),
                command.copyright(),
                command.uri(),
                command.creator(),
                command.userId()
        );
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Image ajoutée avec succès");
    }

    @Override
    public MutationResult updateImage(UpdateConceptImageCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Aucune image sélectionnée !");
        }
        imageService.updateExternalImage(
                command.conceptId(),
                command.thesaurusId(),
                NodeImage.builder()
                        .id(command.imageId())
                        .uri(command.uri())
                        .imageName(command.name())
                        .creator(command.creator())
                        .copyRight(command.copyright())
                        .build()
        );
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("L'URI du l'image est modifiée avec succès");
    }

    @Override
    public MutationResult deleteImage(DeleteConceptImageCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        imageService.deleteImages(command.thesaurusId(), command.conceptId(), command.uri());
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Image supprimée avec succès");
    }

    @Override
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
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Ressource ajoutée avec succès");
    }

    @Override
    public MutationResult updateExternalResource(UpdateExternalResourceCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Pas de sélection !");
        }
        if (!StringUtils.urlValidator(command.uri())) {
            return MutationResult.validationError("L'URL n'est pas valide !");
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

    @Override
    public MutationResult deleteExternalResource(DeleteExternalResourceCommand command) {
        if (ObjectUtils.isEmpty(command.uri())) {
            return MutationResult.validationError("Aucune resource n'est sélectionnée !");
        }
        externalResourcesRepository.deleteExternalResource(
                command.thesaurusId(),
                command.conceptId(),
                command.uri()
        );
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Ressource supprimée avec succès");
    }

    private void touchConcept(ReplaceGpsCoordinatesCommand command) {
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
    }

    private void touchConcept(String thesaurusId, String conceptId, int userId, String contributorName) {
        conceptService.updateDateOfConcept(thesaurusId, conceptId, userId);
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(contributorName)
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .build());
    }
}
