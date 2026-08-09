package fr.cnrs.opentheso.v2.concept.write.service;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.v2.concept.alignment.support.AlignmentUrlProbe;
import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteAlignmentType;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddManualAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateAlignmentCommand;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptWritePostMutationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Gestion (lecture des types + CRUD manuel) des alignements SKOS d'un concept, indépendante
 * du bean/service legacy : ne s'appuie que sur les repositories JPA {@link AlignementRepository}
 * et {@link AlignementTypeRepository}.
 */
@Service
@RequiredArgsConstructor
public class ConceptAlignmentMutationService {

    private final AlignementRepository alignementRepository;
    private final AlignementTypeRepository alignementTypeRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @Transactional(readOnly = true)
    public List<ConceptWriteAlignmentType> listAlignmentTypes() {
        return alignementTypeRepository.findAll().stream()
                .map(type -> new ConceptWriteAlignmentType(type.getId(), type.getLabel(), type.getLabelSkos()))
                .sorted((a, b) -> StringUtils.compareIgnoreCase(a.getLabel(), b.getLabel()))
                .toList();
    }

    @Transactional
    public MutationResult addManualAlignment(AddManualAlignmentCommand command) {
        String uri = StringUtils.trimToEmpty(command.uri());
        if (StringUtils.isBlank(uri)) {
            return MutationResult.validationError("Veuillez saisir une valeur  !");
        }
        if (!AlignmentUrlProbe.isValidFormat(uri)) {
            return MutationResult.validationError("L'URL n'est pas valide !");
        }
        if (command.typeId() <= 0) {
            return MutationResult.validationError("Le type d'alignement est obligatoire !");
        }
        var alignementType = alignementTypeRepository.findById(command.typeId());
        if (alignementType.isEmpty()) {
            return MutationResult.validationError("Le type d'alignement est introuvable !");
        }
        uri = fr.cnrs.opentheso.utils.StringUtils.convertString(uri);
        String source = fr.cnrs.opentheso.utils.StringUtils.convertString(
                StringUtils.trimToEmpty(command.source()));

        if (alignementRepository.existsByConceptThesaurusTypeAndUri(
                command.thesaurusId(), command.conceptId(), command.typeId(), uri)) {
            return MutationResult.duplicate("Cet alignement existe déjà pour ce concept !");
        }
        if (alignementRepository.existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(
                command.thesaurusId(), command.conceptId(), uri)) {
            return MutationResult.duplicate("Cette URI est déjà alignée avec un autre type d'équivalence !");
        }

        if (!AlignmentUrlProbe.isReachable(uri)) {
            return MutationResult.validationError("L'URL n'est pas joignable !");
        }

        var now = new Date();
        try {
            alignementRepository.save(Alignement.builder()
                    .author(command.userId())
                    .conceptTarget("")
                    .thesaurusTarget(source)
                    .uriTarget(uri)
                    .urlAvailable(true)
                    .alignementType(alignementType.get())
                    .internalIdConcept(command.conceptId())
                    .internalIdThesaurus(command.thesaurusId())
                    .created(now)
                    .modified(now)
                    .build());
        } catch (DataIntegrityViolationException e) {
            return MutationResult.duplicate("Cette URI est déjà alignée sur ce concept !");
        }

        return finalizeMutation(
                command.thesaurusId(),
                command.conceptId(),
                command.userId(),
                command.contributorName(),
                "Alignement ajouté avec succès"
        );
    }

    @Transactional
    public MutationResult updateAlignment(UpdateAlignmentCommand command) {
        String uri = StringUtils.trimToEmpty(command.uri());
        if (StringUtils.isBlank(uri)) {
            return MutationResult.validationError("Veuillez saisir une valeur  !");
        }
        if (!AlignmentUrlProbe.isValidFormat(uri)) {
            return MutationResult.validationError("L'URL n'est pas valide !");
        }
        if (command.typeId() <= 0) {
            return MutationResult.validationError("Le type d'alignement est obligatoire !");
        }
        var alignement = alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId(
                command.thesaurusId(), command.conceptId(), command.alignmentId());
        if (alignement.isEmpty()) {
            return MutationResult.validationError("Alignement introuvable !");
        }
        var alignementType = alignementTypeRepository.findById(command.typeId());
        if (alignementType.isEmpty()) {
            return MutationResult.validationError("Le type d'alignement est introuvable !");
        }

        uri = fr.cnrs.opentheso.utils.StringUtils.convertString(uri);
        String source = fr.cnrs.opentheso.utils.StringUtils.convertString(
                StringUtils.trimToEmpty(command.source()));

        var entity = alignement.get();
        String previousUri = StringUtils.defaultString(entity.getUriTarget());
        if (!uri.equals(previousUri)
                && alignementRepository.existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(
                command.thesaurusId(), command.conceptId(), uri)) {
            return MutationResult.duplicate("Cette URI est déjà alignée sur ce concept !");
        }

        if (!AlignmentUrlProbe.isReachable(uri)) {
            return MutationResult.validationError("L'URL n'est pas joignable !");
        }

        entity.setUriTarget(uri);
        entity.setThesaurusTarget(source);
        entity.setAlignementType(alignementType.get());
        entity.setUrlAvailable(true);
        entity.setModified(new Date());
        try {
            alignementRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            return MutationResult.duplicate("Cette URI est déjà alignée sur ce concept !");
        }

        return finalizeMutation(
                command.thesaurusId(),
                command.conceptId(),
                command.userId(),
                command.contributorName(),
                "Alignement modifié avec succès"
        );
    }

    @Transactional
    public MutationResult deleteAlignment(DeleteAlignmentCommand command) {
        var alignement = alignementRepository.findByInternalIdThesaurusAndInternalIdConceptAndId(
                command.thesaurusId(), command.conceptId(), command.alignmentId());
        if (alignement.isEmpty()) {
            return MutationResult.validationError("Alignement introuvable !");
        }
        alignementRepository.delete(alignement.get());

        return finalizeMutation(
                command.thesaurusId(),
                command.conceptId(),
                command.userId(),
                command.contributorName(),
                "Alignement supprimé avec succès"
        );
    }

    private MutationResult finalizeMutation(
            String thesaurusId,
            String conceptId,
            int userId,
            String contributorName,
            String successMessage
    ) {
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
        conceptWritePostMutationRepository.saveContributorDcTerm(
                thesaurusId, conceptId, StringUtils.defaultString(contributorName));
        return MutationResult.ok(successMessage);
    }
}
