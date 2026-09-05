package fr.cnrs.opentheso.v2.collection.write.service;

import fr.cnrs.opentheso.entites.ConceptGroup;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.ConceptGroupLabel;
import fr.cnrs.opentheso.entites.RelationGroup;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupRepository;
import fr.cnrs.opentheso.repositories.RelationGroupRepository;
import fr.cnrs.opentheso.v2.collection.identifier.CollectionIdentifierAssignmentService;
import fr.cnrs.opentheso.v2.collection.write.model.command.AddCollectionTranslationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.AddMemberToCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.CreateCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.CreateSubgroupCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.DeleteCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.DeleteCollectionTranslationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.MoveCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RemoveAllMembersFromCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RemoveMemberFromCollectionCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.RenameCollectionLabelCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionNotationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionTranslationCommand;
import fr.cnrs.opentheso.v2.collection.write.model.command.UpdateCollectionTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CollectionMutationService {

    private static final String COLLECTION_NOT_FOUND = "Collection introuvable !";
    private static final String LABEL_REQUIRED = "Le libellé est obligatoire !";
    private static final String NOTATION_DUPLICATE = "Cette notation existe déjà dans le thésaurus !";


    private final ConceptGroupRepository conceptGroupRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final RelationGroupRepository relationGroupRepository;
    private final BranchConceptSupport branchConceptSupport;
    private final CollectionIdentifierAssignmentService collectionIdentifierAssignmentService;

    @Transactional
    public MutationResult renamePreferredLabel(RenameCollectionLabelCommand command) {
        if (StringUtils.isBlank(command.label())) {
            return MutationResult.validationError(LABEL_REQUIRED);
        }
        var labels = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(
                command.thesaurusId(), command.collectionId(), command.lang());
        if (labels.isEmpty()) {
            return MutationResult.validationError(COLLECTION_NOT_FOUND);
        }
        var label = labels.get(0);
        label.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(command.label()));
        label.setModified(new Date());
        conceptGroupLabelRepository.save(label);
        touchCollection(command.thesaurusId(), command.collectionId());
        return MutationResult.ok("La collection a bien été modifiée");
    }

    @Transactional
    public MutationResult updateNotation(UpdateCollectionNotationCommand command) {
        Optional<ConceptGroup> group = conceptGroupRepository.findByIdGroupAndIdThesaurus(
                command.collectionId(), command.thesaurusId());
        if (group.isEmpty()) {
            return MutationResult.validationError(COLLECTION_NOT_FOUND);
        }
        String notation = StringUtils.defaultString(command.notation());
        if (StringUtils.isNotBlank(notation) && isNotationUsedByOtherCollection(
                command.thesaurusId(), notation, command.collectionId())) {
            return MutationResult.duplicate(NOTATION_DUPLICATE);
        }
        group.get().setNotation(notation);
        group.get().setModified(new Date());
        conceptGroupRepository.save(group.get());
        return MutationResult.ok("La notation a bien été modifiée");
    }

    @Transactional
    public MutationResult updateType(UpdateCollectionTypeCommand command) {
        if (StringUtils.isBlank(command.typeCode())) {
            return MutationResult.validationError("Le type est obligatoire !");
        }
        Optional<ConceptGroup> group = conceptGroupRepository.findByIdGroupAndIdThesaurus(
                command.collectionId(), command.thesaurusId());
        if (group.isEmpty()) {
            return MutationResult.validationError(COLLECTION_NOT_FOUND);
        }
        group.get().setIdTypeCode(command.typeCode());
        group.get().setModified(new Date());
        conceptGroupRepository.save(group.get());
        return MutationResult.ok("Le type a bien été modifié");
    }

    @Transactional
    public MutationResult deleteCollection(DeleteCollectionCommand command) {
        Optional<ConceptGroup> group = conceptGroupRepository.findByIdGroupAndIdThesaurus(
                command.collectionId(), command.thesaurusId());
        if (group.isEmpty()) {
            return MutationResult.validationError(COLLECTION_NOT_FOUND);
        }
        conceptGroupConceptRepository.deleteAllByIdGroupAndIdThesaurus(command.collectionId(), command.thesaurusId());
        conceptGroupLabelRepository.deleteByIdThesaurusAndIdGroup(command.thesaurusId(), command.collectionId());
        relationGroupRepository.deleteByIdThesaurusAndIdGroup2(command.thesaurusId(), command.collectionId());
        conceptGroupRepository.deleteByIdThesaurusAndIdGroup(command.thesaurusId(), command.collectionId());
        return MutationResult.ok("La collection a bien été supprimée");
    }

    @Transactional
    public MutationResult addTranslation(AddCollectionTranslationCommand command) {
        if (StringUtils.isAnyBlank(command.lang(), command.label())) {
            return MutationResult.validationError("La langue et le libellé sont obligatoires !");
        }
        if (!conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(
                command.thesaurusId(), command.collectionId(), command.lang()).isEmpty()) {
            return MutationResult.duplicate("Une traduction existe déjà pour cette langue !");
        }
        conceptGroupLabelRepository.save(ConceptGroupLabel.builder()
                .idGroup(command.collectionId().toLowerCase())
                .idThesaurus(command.thesaurusId())
                .lang(command.lang())
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(command.label()))
                .created(new Date())
                .modified(new Date())
                .build());
        touchCollection(command.thesaurusId(), command.collectionId());
        return MutationResult.ok("Traduction ajoutée avec succès");
    }

    @Transactional
    public MutationResult updateTranslation(UpdateCollectionTranslationCommand command) {
        if (StringUtils.isAnyBlank(command.lang(), command.label())) {
            return MutationResult.validationError("La langue et le libellé sont obligatoires !");
        }
        var labels = conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(
                command.thesaurusId(), command.collectionId(), command.lang());
        if (labels.isEmpty()) {
            return MutationResult.validationError("Traduction introuvable !");
        }
        var label = labels.get(0);
        label.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(command.label()));
        label.setModified(new Date());
        conceptGroupLabelRepository.save(label);
        touchCollection(command.thesaurusId(), command.collectionId());
        return MutationResult.ok("Traduction modifiée avec succès");
    }

    @Transactional
    public MutationResult deleteTranslation(DeleteCollectionTranslationCommand command) {
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("La langue est obligatoire !");
        }
        if (conceptGroupLabelRepository.findAllByIdThesaurusAndIdGroupAndLang(
                command.thesaurusId(), command.collectionId(), command.lang()).isEmpty()) {
            return MutationResult.validationError("Traduction introuvable !");
        }
        conceptGroupLabelRepository.deleteAllByIdGroupAndIdThesaurusAndLang(
                command.collectionId(), command.thesaurusId(), command.lang());
        touchCollection(command.thesaurusId(), command.collectionId());
        return MutationResult.ok("Traduction supprimée avec succès");
    }

    @Transactional
    public MutationResult addMember(AddMemberToCollectionCommand command) {
        if (StringUtils.isAnyBlank(command.collectionId(), command.conceptId())) {
            return MutationResult.validationError("Sélection invalide !");
        }
        List<String> conceptIds = command.applyToBranch()
                ? branchConceptSupport.collectBranchConceptIds(command.thesaurusId(), command.conceptId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("Aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                    .idGroup(command.collectionId().toLowerCase())
                    .idThesaurus(command.thesaurusId())
                    .idConcept(conceptId)
                    .build());
        }
        touchCollection(command.thesaurusId(), command.collectionId());
        return MutationResult.ok(command.applyToBranch()
                ? "La branche a bien été ajoutée à la collection"
                : "Le concept a été ajouté à la collection");
    }

    @Transactional
    public MutationResult removeMember(RemoveMemberFromCollectionCommand command) {
        List<String> conceptIds = command.applyToBranch()
                ? branchConceptSupport.collectBranchConceptIds(command.thesaurusId(), command.conceptId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("Aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(
                    command.collectionId(), conceptId, command.thesaurusId());
        }
        touchCollection(command.thesaurusId(), command.collectionId());
        return MutationResult.ok(command.applyToBranch()
                ? "La branche a bien été enlevée de la collection"
                : "Le concept a bien été enlevé de la collection");
    }

    @Transactional
    public MutationResult removeAllMembers(RemoveAllMembersFromCollectionCommand command) {
        conceptGroupConceptRepository.deleteAllByIdGroupAndIdThesaurus(
                command.collectionId(), command.thesaurusId());
        touchCollection(command.thesaurusId(), command.collectionId());
        return MutationResult.ok("Tous les concepts ont été retirés de la collection");
    }

    @Transactional
    public MutationResult createCollection(CreateCollectionCommand command) {
        if (StringUtils.isBlank(command.label())) {
            return MutationResult.validationError(LABEL_REQUIRED);
        }
        if (StringUtils.isNotBlank(command.notation())
                && isNotationUsedByOtherCollection(command.thesaurusId(), command.notation(), null)) {
            return MutationResult.duplicate(NOTATION_DUPLICATE);
        }
        String collectionId = createGroupEntity(
                command.thesaurusId(),
                command.lang(),
                command.label(),
                command.notation(),
                StringUtils.defaultIfBlank(command.typeCode(), "MT")
        );
        collectionIdentifierAssignmentService.assignOnCreation(
                command.thesaurusId(), collectionId, command.label());
        return MutationResult.ok("La collection a bien été créée", collectionId);
    }

    @Transactional
    public MutationResult createSubgroup(CreateSubgroupCommand command) {
        if (StringUtils.isBlank(command.label())) {
            return MutationResult.validationError(LABEL_REQUIRED);
        }
        if (StringUtils.isBlank(command.parentCollectionId())) {
            return MutationResult.validationError("Collection parente obligatoire !");
        }
        if (StringUtils.isNotBlank(command.notation())
                && isNotationUsedByOtherCollection(command.thesaurusId(), command.notation(), null)) {
            return MutationResult.duplicate(NOTATION_DUPLICATE);
        }
        String collectionId = createGroupEntity(
                command.thesaurusId(),
                command.lang(),
                command.label(),
                command.notation(),
                StringUtils.defaultIfBlank(command.typeCode(), "MT")
        );
        relationGroupRepository.save(RelationGroup.builder()
                .idGroup1(command.parentCollectionId().toLowerCase())
                .idGroup2(collectionId.toLowerCase())
                .idThesaurus(command.thesaurusId())
                .relation("sub")
                .build());
        collectionIdentifierAssignmentService.assignOnCreation(
                command.thesaurusId(), collectionId, command.label());
        return MutationResult.ok("La sous-collection a bien été créée", collectionId);
    }

    @Transactional
    public MutationResult moveCollection(MoveCollectionCommand command) {
        String collectionId = command.collectionId();
        String currentParent = relationGroupRepository
                .findByIdThesaurusAndIdGroup2AndRelation(command.thesaurusId(), collectionId, "sub")
                .map(RelationGroup::getIdGroup1)
                .orElse(null);

        if (command.moveToRoot()) {
            if (StringUtils.isBlank(currentParent)) {
                return MutationResult.validationError("Déplacement à la même place !");
            }
            relationGroupRepository.deleteByIdGroup1AndIdGroup2AndIdThesaurus(
                    currentParent, collectionId, command.thesaurusId());
            touchCollection(command.thesaurusId(), collectionId);
            return MutationResult.ok("Déplacement réussi !");
        }

        if (StringUtils.isBlank(command.targetParentCollectionId())) {
            return MutationResult.validationError("Pas de sélection !");
        }
        if (collectionId.equalsIgnoreCase(command.targetParentCollectionId())) {
            return MutationResult.validationError("Déplacement impossible !");
        }
        if (isMoveToDescending(collectionId, command.targetParentCollectionId(), command.thesaurusId())) {
            return MutationResult.validationError("Déplacement impossible !");
        }
        if (StringUtils.isNotBlank(currentParent)
                && currentParent.equalsIgnoreCase(command.targetParentCollectionId())) {
            return MutationResult.validationError("Déplacement à la même place !");
        }
        if (StringUtils.isNotBlank(currentParent)) {
            relationGroupRepository.deleteByIdGroup1AndIdGroup2AndIdThesaurus(
                    currentParent, collectionId, command.thesaurusId());
        }
        relationGroupRepository.save(RelationGroup.builder()
                .idGroup1(command.targetParentCollectionId().toLowerCase())
                .idGroup2(collectionId.toLowerCase())
                .idThesaurus(command.thesaurusId())
                .relation("sub")
                .build());
        touchCollection(command.thesaurusId(), collectionId);
        return MutationResult.ok("Déplacement réussi !");
    }

    private String createGroupEntity(
            String thesaurusId,
            String lang,
            String label,
            String notation,
            String typeCode
    ) {
        Long nextId = conceptGroupRepository.getNextConceptGroupSequence();
        String collectionId = "g" + nextId;
        conceptGroupRepository.save(ConceptGroup.builder()
                .id(nextId.intValue())
                .idGroup(collectionId)
                .idArk("")
                .idThesaurus(thesaurusId)
                .idTypeCode(typeCode)
                .notation(StringUtils.defaultString(notation))
                .idHandle("")
                .idDoi("")
                .created(new Date())
                .modified(new Date())
                .build());
        conceptGroupLabelRepository.save(ConceptGroupLabel.builder()
                .idGroup(collectionId)
                .idThesaurus(thesaurusId)
                .lang(lang)
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(label))
                .created(new Date())
                .modified(new Date())
                .build());
        return collectionId;
    }

    private void touchCollection(String thesaurusId, String collectionId) {
        conceptGroupRepository.updateModifiedDate(collectionId.toLowerCase(), thesaurusId);
    }

    private boolean isMoveToDescending(String collectionId, String targetParentId, String thesaurusId) {
        return collectDescendants(collectionId, thesaurusId).contains(targetParentId);
    }

    private List<String> collectDescendants(String collectionId, String thesaurusId) {
        List<String> allIds = new ArrayList<>();
        collectDescendantsRecursive(collectionId, thesaurusId, allIds);
        return allIds;
    }

    private void collectDescendantsRecursive(String collectionId, String thesaurusId, List<String> allIds) {
        allIds.add(collectionId);
        List<String> children = relationGroupRepository.findChildGroupIds(thesaurusId, collectionId);
        if (CollectionUtils.isEmpty(children)) {
            return;
        }
        for (String childId : children) {
            collectDescendantsRecursive(childId, thesaurusId, allIds);
        }
    }

    private boolean isNotationUsedByOtherCollection(String thesaurusId, String notation, String excludeCollectionId) {
        if (StringUtils.isBlank(notation)) {
            return false;
        }
        return conceptGroupRepository.findByIdThesaurusAndNotation(thesaurusId, notation).stream()
                .anyMatch(group -> excludeCollectionId == null
                        || !group.getIdGroup().equalsIgnoreCase(excludeCollectionId));
    }
}
