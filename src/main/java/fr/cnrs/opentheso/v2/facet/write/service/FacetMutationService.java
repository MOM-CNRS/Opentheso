package fr.cnrs.opentheso.v2.facet.write.service;

import fr.cnrs.opentheso.entites.ConceptFacet;
import fr.cnrs.opentheso.entites.NodeLabel;
import fr.cnrs.opentheso.entites.ThesaurusArray;
import fr.cnrs.opentheso.repositories.ConceptFacetRepository;
import fr.cnrs.opentheso.repositories.NodeLabelRepository;
import fr.cnrs.opentheso.repositories.ThesaurusArrayRepository;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.persistence.BranchConceptSupport;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.AddFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.CreateFacetCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.DeleteFacetCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.DeleteFacetTranslationCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveAllFacetMembersCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RemoveFacetMemberCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.RenameFacetLabelCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.UpdateFacetParentCommand;
import fr.cnrs.opentheso.v2.facet.write.model.command.UpdateFacetTranslationCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacetMutationService {

    private final ThesaurusArrayRepository thesaurusArrayRepository;
    private final ConceptFacetRepository conceptFacetRepository;
    private final NodeLabelRepository nodeLabelRepository;
    private final BranchConceptSupport branchConceptSupport;

    @Transactional
    public MutationResult renamePreferredLabel(RenameFacetLabelCommand command) {
        if (StringUtils.isBlank(command.label())) {
            return MutationResult.validationError("Le libellé est obligatoire !");
        }
        var label = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(
                command.facetId(), command.thesaurusId(), command.lang());
        if (label.isEmpty()) {
            return MutationResult.validationError("Facette introuvable !");
        }
        var entity = label.get();
        entity.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(command.label()));
        entity.setModified(new Date());
        nodeLabelRepository.save(entity);
        return MutationResult.ok("La facette a bien été modifiée");
    }

    @Transactional
    public MutationResult deleteFacet(DeleteFacetCommand command) {
        thesaurusArrayRepository.deleteAllByIdThesaurusAndIdFacet(command.thesaurusId(), command.facetId());
        conceptFacetRepository.deleteAllByIdThesaurusAndIdFacet(command.thesaurusId(), command.facetId());
        nodeLabelRepository.deleteAllByIdThesaurusAndIdFacet(command.thesaurusId(), command.facetId());
        return MutationResult.ok("La facette a bien été supprimée");
    }

    @Transactional
    public MutationResult updateParent(UpdateFacetParentCommand command) {
        if (StringUtils.isBlank(command.parentConceptId())) {
            return MutationResult.validationError("Concept parent obligatoire !");
        }
        thesaurusArrayRepository.updateConceptParent(
                command.parentConceptId(),
                command.thesaurusId(),
                command.facetId()
        );
        return MutationResult.ok("Le concept parent a bien été modifié");
    }

    @Transactional
    public MutationResult addMember(AddFacetMemberCommand command) {
        if (StringUtils.isAnyBlank(command.facetId(), command.conceptId())) {
            return MutationResult.validationError("Sélection invalide !");
        }
        List<String> conceptIds = command.applyToBranch()
                ? branchConceptSupport.collectBranchConceptIds(command.thesaurusId(), command.conceptId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("Aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptFacetRepository.save(ConceptFacet.builder()
                    .idFacet(command.facetId())
                    .idThesaurus(command.thesaurusId())
                    .idConcept(conceptId)
                    .build());
        }
        return MutationResult.ok(command.applyToBranch()
                ? "La branche a bien été ajoutée à la facette"
                : "Le concept a été ajouté à la facette");
    }

    @Transactional
    public MutationResult removeMember(RemoveFacetMemberCommand command) {
        List<String> conceptIds = command.applyToBranch()
                ? branchConceptSupport.collectBranchConceptIds(command.thesaurusId(), command.conceptId())
                : List.of(command.conceptId());
        if (CollectionUtils.isEmpty(conceptIds)) {
            return MutationResult.validationError("Aucun concept sélectionné !");
        }
        for (String conceptId : conceptIds) {
            conceptFacetRepository.deleteAllByIdConceptAndIdThesaurusAndIdFacet(
                    conceptId, command.thesaurusId(), command.facetId());
        }
        return MutationResult.ok(command.applyToBranch()
                ? "La branche a bien été enlevée de la facette"
                : "Le concept a bien été enlevé de la facette");
    }

    @Transactional
    public MutationResult removeAllMembers(RemoveAllFacetMembersCommand command) {
        conceptFacetRepository.deleteAllByIdThesaurusAndIdFacet(command.thesaurusId(), command.facetId());
        return MutationResult.ok("Tous les concepts ont été retirés de la facette");
    }

    @Transactional
    public MutationResult addTranslation(AddFacetTranslationCommand command) {
        if (StringUtils.isAnyBlank(command.lang(), command.label())) {
            return MutationResult.validationError("La langue et le libellé sont obligatoires !");
        }
        if (nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(
                command.facetId(), command.thesaurusId(), command.lang()).isPresent()) {
            return MutationResult.duplicate("Une traduction existe déjà pour cette langue !");
        }
        nodeLabelRepository.save(NodeLabel.builder()
                .idFacet(command.facetId())
                .idThesaurus(command.thesaurusId())
                .lang(command.lang())
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(command.label()))
                .created(new Date())
                .modified(new Date())
                .build());
        return MutationResult.ok("Traduction ajoutée avec succès");
    }

    @Transactional
    public MutationResult updateTranslation(UpdateFacetTranslationCommand command) {
        if (StringUtils.isAnyBlank(command.lang(), command.label())) {
            return MutationResult.validationError("La langue et le libellé sont obligatoires !");
        }
        var label = nodeLabelRepository.findByIdFacetAndIdThesaurusAndLang(
                command.facetId(), command.thesaurusId(), command.lang());
        if (label.isEmpty()) {
            return MutationResult.validationError("Traduction introuvable !");
        }
        var entity = label.get();
        entity.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(command.label()));
        entity.setModified(new Date());
        nodeLabelRepository.save(entity);
        return MutationResult.ok("Traduction modifiée avec succès");
    }

    @Transactional
    public MutationResult deleteTranslation(DeleteFacetTranslationCommand command) {
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("La langue est obligatoire !");
        }
        nodeLabelRepository.deleteAllByIdThesaurusAndIdFacetAndLang(
                command.thesaurusId(), command.facetId(), command.lang());
        return MutationResult.ok("Traduction supprimée avec succès");
    }

    @Transactional
    public MutationResult createFacet(CreateFacetCommand command) {
        if (StringUtils.isAnyBlank(command.label(), command.parentConceptId())) {
            return MutationResult.validationError("Le libellé et le concept parent sont obligatoires !");
        }
        String lexicalValue = fr.cnrs.opentheso.utils.StringUtils.convertString(command.label());
        if (StringUtils.isBlank(lexicalValue)) {
            return MutationResult.validationError("Le libellé et le concept parent sont obligatoires !");
        }
        if (nodeLabelRepository.existsByIdThesaurusAndLexicalValueAndLang(
                command.thesaurusId(), lexicalValue, command.lang())) {
            return MutationResult.duplicate("Le nom de la facette '" + lexicalValue + "' existe déjà !");
        }
        String facetId = generateFacetId();
        nodeLabelRepository.save(NodeLabel.builder()
                .idFacet(facetId)
                .idThesaurus(command.thesaurusId())
                .lang(command.lang())
                .lexicalValue(lexicalValue)
                .created(new Date())
                .modified(new Date())
                .build());
        thesaurusArrayRepository.save(ThesaurusArray.builder()
                .idThesaurus(command.thesaurusId())
                .idConceptParent(command.parentConceptId())
                .idFacet(facetId)
                .build());
        return MutationResult.ok("La facette a bien été créée", facetId);
    }

    private String generateFacetId() {
        Long nextId = thesaurusArrayRepository.getNextFacetSequenceId();
        if (nextId == null) {
            throw new IllegalStateException("Impossible de générer un nouvel ID de facette.");
        }
        String facetId = "F" + nextId;
        while (conceptFacetRepository.findByIdFacet(facetId).isPresent()) {
            nextId = thesaurusArrayRepository.getNextFacetSequenceId();
            facetId = "F" + nextId;
        }
        return facetId;
    }
}
