package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.entites.ExternalResource;
import fr.cnrs.opentheso.entites.Gps;
import fr.cnrs.opentheso.entites.ImageExterne;
import fr.cnrs.opentheso.repositories.ExternalResourcesRepository;
import fr.cnrs.opentheso.repositories.GpsRepository;
import fr.cnrs.opentheso.repositories.ImagesRepository;
import fr.cnrs.opentheso.utils.StringUtils;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ReplaceGpsCoordinatesCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateConceptImageCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateExternalResourceCommand;
import fr.cnrs.opentheso.v2.concept.write.support.ConceptGpsCoordinateParser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConceptMediaWritePersistence {

    private final GpsRepository gpsRepository;
    private final ImagesRepository imagesRepository;
    private final ExternalResourcesRepository externalResourcesRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    public MutationResult replaceGpsCoordinates(ReplaceGpsCoordinatesCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.coordinatesText())) {
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
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Image ajoutée avec succès");
    }

    public MutationResult updateImage(UpdateConceptImageCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Aucune image sélectionnée !");
        }
        imagesRepository.save(ImageExterne.builder()
                .id(command.imageId())
                .imageCreator(command.creator())
                .externalUri(command.uri())
                .imageName(command.name())
                .imageCopyright(command.copyright())
                .idThesaurus(command.thesaurusId())
                .idConcept(command.conceptId())
                .build());
        touchConcept(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("L'URI du l'image est modifiée avec succès");
    }

    public MutationResult deleteImage(DeleteConceptImageCommand command) {
        if (org.apache.commons.lang3.StringUtils.isBlank(command.uri())) {
            return MutationResult.validationError("Aucune sélection !");
        }
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
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(contributorName)) {
            conceptWritePostMutationRepository.saveContributorDcTerm(thesaurusId, conceptId, contributorName);
        }
    }
}
