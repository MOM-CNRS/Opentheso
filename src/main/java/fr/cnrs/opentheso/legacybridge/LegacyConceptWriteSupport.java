package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.services.ConceptAddService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.ConceptTypeService;
import fr.cnrs.opentheso.services.NonPreferredTermService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddChildConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTopConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApplyNarrowerRelationToBranchCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.ApproveConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteBroaderRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteCustomRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNarrowerRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteRelatedRelationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteReplacedByCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeprecateConceptCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.RenamePreferredLabelCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateNarrowerRelationTypeCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpsertNoteCommand;
import fr.cnrs.opentheso.v2.concept.write.session.ConceptWritePort;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class LegacyConceptWriteSupport implements ConceptWritePort {

    private static final String DEFAULT_STATUS = "D";

    private final TermRepository termRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final ConceptDcTermRepository conceptDcTermRepository;
    private final TermService termService;
    private final ConceptService conceptService;
    private final ConceptAddService conceptAddService;
    private final RelationService relationService;
    private final NonPreferredTermService nonPreferredTermService;
    private final NoteService noteService;
    private final ConceptTypeService conceptTypeService;

    @Override
    public MutationResult renamePreferredLabel(RenamePreferredLabelCommand command) {
        if (StringUtils.isBlank(command.label())) {
            return MutationResult.validationError("Le label est obligatoire !");
        }

        if (!command.forced()) {
            String normalized = fr.cnrs.opentheso.utils.StringUtils.convertString(command.label().trim());
            var existing = termRepository.findByLexicalValueAndLangAndIdThesaurus(
                    normalized, command.lang(), command.thesaurusId());
            if (existing.isPresent() && !isSameConceptTerm(existing.get(), command)) {
                return MutationResult.duplicate(
                        "Le label '" + existing.get().getLexicalValue() + "' existe déjà ! voulez-vous continuer ?");
            }
        }

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(
                command.thesaurusId(), command.conceptId());
        String idTerm = preferredTerm.map(fr.cnrs.opentheso.entites.PreferredTerm::getIdTerm).orElse(null);

        if (idTerm == null) {
            var term = Term.builder()
                    .idThesaurus(command.thesaurusId())
                    .lang(command.lang())
                    .status("")
                    .lexicalValue(command.label().trim())
                    .source(StringUtils.defaultString(command.source()))
                    .build();
            termService.addTerm(term, command.conceptId(), command.userId());
        } else if (termService.isTermExistInLangAndThesaurus(idTerm, command.thesaurusId(), command.lang())) {
            termService.updateTermTraduction(
                    command.label(), idTerm, command.lang(), command.thesaurusId(), command.userId());
        } else {
            var term = Term.builder()
                    .lexicalValue(command.label().trim())
                    .idTerm(idTerm)
                    .lang(command.lang())
                    .idThesaurus(command.thesaurusId())
                    .source("")
                    .status("")
                    .created(new Date())
                    .modified(new Date())
                    .build();
            termService.addTermTraduction(term, command.userId());
        }

        conceptService.updateDateOfConcept(command.thesaurusId(), command.conceptId(), command.userId());
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(command.contributorName())
                .idConcept(command.conceptId())
                .idThesaurus(command.thesaurusId())
                .build());

        return MutationResult.ok("Le concept a bien été modifié");
    }

    @Override
    public MutationResult addChildConcept(AddChildConceptCommand command) {
        MutationResult validation = validateNewConceptFields(
                command.thesaurusId(),
                command.lang(),
                command.preferredLabel(),
                command.notation(),
                command.customConceptId(),
                command.forcedDuplicate()
        );
        if (validation != null) {
            return validation;
        }

        var concept = Concept.builder()
                .idGroup(command.groupId())
                .idThesaurus(command.thesaurusId())
                .idConcept(StringUtils.defaultIfBlank(command.customConceptId(), null))
                .topConcept(false)
                .status(DEFAULT_STATUS)
                .notation(command.notation())
                .build();

        var term = Term.builder()
                .idThesaurus(command.thesaurusId())
                .lang(command.lang())
                .status(DEFAULT_STATUS)
                .lexicalValue(command.preferredLabel().trim())
                .source(StringUtils.defaultString(command.source()))
                .build();

        String newConceptId = conceptAddService.addConcept(
                command.parentConceptId(), command.narrowerRelationType(), concept, term, command.userId());
        if (newConceptId == null) {
            return MutationResult.failure("Erreur pendant l'enregistrement du nouveau concept !");
        }

        saveCreatorDcTerm(command.thesaurusId(), newConceptId, command.contributorName());
        return MutationResult.ok("Le concept a bien été ajouté", newConceptId);
    }

    @Override
    public MutationResult addTopConcept(AddTopConceptCommand command) {
        MutationResult validation = validateNewConceptFields(
                command.thesaurusId(),
                command.lang(),
                command.preferredLabel(),
                command.notation(),
                command.customConceptId(),
                command.forcedDuplicate()
        );
        if (validation != null) {
            return validation;
        }

        var concept = Concept.builder()
                .idGroup(command.groupId())
                .idThesaurus(command.thesaurusId())
                .idConcept(StringUtils.defaultIfBlank(command.customConceptId(), null))
                .status(DEFAULT_STATUS)
                .notation(command.notation())
                .topConcept(false)
                .build();

        var term = Term.builder()
                .idThesaurus(command.thesaurusId())
                .lang(command.lang())
                .status(DEFAULT_STATUS)
                .lexicalValue(command.preferredLabel().trim())
                .source(StringUtils.defaultString(command.source()))
                .build();

        String newConceptId = conceptAddService.addConcept(null, null, concept, term, command.userId());
        if (newConceptId == null) {
            return MutationResult.failure("Erreur pendant la création du concept");
        }

        saveCreatorDcTerm(command.thesaurusId(), newConceptId, command.contributorName());
        return MutationResult.ok("Le top concept a bien été ajouté", newConceptId);
    }

    @Override
    public MutationResult deleteConcept(DeleteConceptCommand command) {
        boolean deleted;
        if (command.hasNarrowers()) {
            deleted = conceptService.deleteBranchConcept(
                    command.conceptId(),
                    command.thesaurusId(),
                    command.forceDeletePolyhierarchy()
            );
            if (!deleted) {
                return MutationResult.failure("La suppression a échoué, vérifier la poly-hiérarchie pour le concept");
            }
        } else {
            deleted = conceptService.deleteConcept(command.conceptId(), command.thesaurusId());
            if (!deleted) {
                return MutationResult.failure("La suppression a échoué !!");
            }
        }
        return MutationResult.ok("Le concept a bien été supprimé");
    }

    @Override
    public MutationResult addBroaderRelation(AddBroaderRelationCommand command) {
        MutationResult validation = validateHierarchicalRelationCommand(
                command.thesaurusId(), command.conceptId(), command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        relationService.addRelationBT(
                command.conceptId(), command.thesaurusId(), command.targetConceptId(), command.userId());

        if (conceptService.isTopConcept(command.conceptId(), command.thesaurusId())
                && !conceptService.setTopConcept(command.conceptId(), command.thesaurusId(), false)) {
            return MutationResult.failure(
                    "Erreur en enlevant le concept du TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Override
    public MutationResult addNarrowerRelation(AddNarrowerRelationCommand command) {
        MutationResult validation = validateHierarchicalRelationCommand(
                command.thesaurusId(), command.conceptId(), command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        relationService.addRelationNT(
                command.conceptId(), command.thesaurusId(), command.targetConceptId(), command.userId());

        if (conceptService.isTopConcept(command.targetConceptId(), command.thesaurusId())
                && !conceptService.setTopConcept(command.targetConceptId(), command.thesaurusId(), false)) {
            return MutationResult.failure(
                    "Erreur en enlevant le concept du TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Override
    public MutationResult deleteBroaderRelation(DeleteBroaderRelationCommand command) {
        MutationResult validation = validateRelationTarget(command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        relationService.deleteRelationBT(
                command.conceptId(), command.thesaurusId(), command.targetConceptId(), command.userId());

        if (!relationService.isConceptHaveRelationBT(command.conceptId(), command.thesaurusId())
                && !conceptService.setTopConcept(command.conceptId(), command.thesaurusId(), true)) {
            return MutationResult.failure(
                    "Erreur en passant le concept en TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    @Override
    public MutationResult deleteNarrowerRelation(DeleteNarrowerRelationCommand command) {
        MutationResult validation = validateRelationTarget(command.targetConceptId());
        if (validation != null) {
            return validation;
        }

        relationService.deleteRelationNT(
                command.conceptId(), command.thesaurusId(), command.targetConceptId(), command.userId());

        if (!relationService.isConceptHaveRelationBT(command.targetConceptId(), command.thesaurusId())
                && !conceptService.setTopConcept(command.targetConceptId(), command.thesaurusId(), true)) {
            return MutationResult.failure(
                    "Erreur en passant le concept en TopConcept, veuillez utiliser les outils de correction de cohérence !");
        }

        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    @Override
    public MutationResult addSynonym(AddSynonymCommand command) {
        if (StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("La valeur est obligatoire !");
        }
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("Pas de langue choisie !");
        }
        if (!command.forced()) {
            MutationResult duplicate = validateSynonymDuplicate(command.thesaurusId(), command.lang(), command.value());
            if (duplicate != null) {
                return duplicate;
            }
        }
        var preferredTerm = termService.getPreferredTermByThesaurusAndConcept(command.thesaurusId(), command.conceptId());
        if (preferredTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        nonPreferredTermService.addNonPreferredTerm(Term.builder()
                .idTerm(preferredTerm.getIdTerm())
                .lexicalValue(command.value())
                .lang(command.lang())
                .idThesaurus(command.thesaurusId())
                .hidden(command.hidden())
                .status(command.hidden() ? "Hidden" : "USE")
                .build(), command.userId());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Synonyme ajouté avec succès");
    }

    @Override
    public MutationResult updateSynonym(UpdateSynonymCommand command) {
        var preferredTerm = termService.getPreferredTermByThesaurusAndConcept(command.thesaurusId(), command.conceptId());
        if (preferredTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        String idTerm = preferredTerm.getIdTerm();
        if (!StringUtils.equals(command.oldValue(), command.newValue())) {
            if (!command.forced()) {
                MutationResult duplicate = validateSynonymDuplicate(command.thesaurusId(), command.lang(), command.newValue());
                if (duplicate != null) {
                    return duplicate;
                }
            }
            if (!nonPreferredTermService.updateNonPreferredTerm(
                    command.oldValue(), command.newValue(), idTerm, command.lang(), command.thesaurusId(),
                    command.hidden(), command.userId())) {
                return MutationResult.failure("La modification a échoué !");
            }
        } else if (!nonPreferredTermService.updateStatusNonPreferredTerm(
                idTerm, command.newValue(), command.lang(), command.thesaurusId(), command.hidden(), command.userId())) {
            return MutationResult.failure("La modification a échoué !");
        }
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Synonyme modifié avec succès");
    }

    @Override
    public MutationResult deleteSynonym(DeleteSynonymCommand command) {
        if (StringUtils.isAnyBlank(command.lang(), command.value())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        var preferredTerm = termService.getPreferredTermByThesaurusAndConcept(command.thesaurusId(), command.conceptId());
        if (preferredTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        nonPreferredTermService.deleteNonPreferredTerm(
                preferredTerm.getIdTerm(), command.lang(), command.value(), command.thesaurusId(), command.userId());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Synonyme supprimé avec succès");
    }

    @Override
    public MutationResult addTranslation(AddTranslationCommand command) {
        if (StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("La valeur est obligatoire !");
        }
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("Aucune langue sélectionnée !");
        }
        if (termService.isTermExistIgnoreCase(command.value(), command.thesaurusId(), command.lang())) {
            return MutationResult.validationError("Un label identique existe dans cette langue !");
        }
        var preferredTerm = termService.getPreferredTermByThesaurusAndConcept(command.thesaurusId(), command.conceptId());
        if (preferredTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        termService.addTermTraduction(Term.builder()
                .idTerm(preferredTerm.getIdTerm())
                .lexicalValue(command.value())
                .lang(command.lang())
                .idThesaurus(command.thesaurusId())
                .creator(command.userId())
                .source("")
                .status("")
                .build(), command.userId());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Traduction ajoutée avec succès");
    }

    @Override
    public MutationResult updateTranslation(UpdateTranslationCommand command) {
        if (StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("Veuillez saisir une valeur !");
        }
        if (termService.isTermExistIgnoreCase(command.value(), command.thesaurusId(), command.lang())) {
            return MutationResult.validationError("Un label identique existe dans cette langue !");
        }
        var preferredTerm = termService.getPreferredTermByThesaurusAndConcept(command.thesaurusId(), command.conceptId());
        if (preferredTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        termService.updateTermTraduction(
                command.value(), preferredTerm.getIdTerm(), command.lang(), command.thesaurusId(), command.userId());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Traduction modifiée avec succès");
    }

    @Override
    public MutationResult deleteTranslation(DeleteTranslationCommand command) {
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("Erreur de sélection de traduction !");
        }
        var preferredTerm = termService.getPreferredTermByThesaurusAndConcept(command.thesaurusId(), command.conceptId());
        if (preferredTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        termService.deleteTerm(command.thesaurusId(), preferredTerm.getIdTerm(), command.lang());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Traduction supprimée avec succès");
    }

    @Override
    public MutationResult upsertNote(UpsertNoteCommand command) {
        if (StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("La note ne doit pas être vide !");
        }
        String cleanedValue = fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(
                fr.cnrs.opentheso.utils.StringUtils.clearValue(command.value()));
        String cleanedSource = fr.cnrs.opentheso.utils.StringUtils.clearValue(StringUtils.defaultString(command.source()));
        if (noteService.isNoteExistInThatLang(
                command.conceptId(), command.thesaurusId(), command.lang(), command.typeCode())) {
            var existing = noteService.getNodeNote(
                    command.conceptId(), command.thesaurusId(), command.lang(), command.typeCode());
            if (existing == null || existing.getIdNote() <= 0) {
                return MutationResult.failure("Erreur de modification !");
            }
            if (!noteService.updateNote(
                    existing.getIdNote(), command.conceptId(), command.lang(), command.thesaurusId(),
                    cleanedValue, cleanedSource, command.typeCode(), command.userId())) {
                return MutationResult.failure("Erreur de modification !");
            }
        } else {
            if (noteService.isNoteExist(
                    command.conceptId(), command.thesaurusId(), command.lang(), cleanedValue, command.typeCode())) {
                return MutationResult.validationError("Cette note existe déjà !");
            }
            noteService.addNote(
                    command.conceptId(), command.lang(), command.thesaurusId(),
                    cleanedValue, command.typeCode(), cleanedSource, command.userId());
        }
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Note enregistrée avec succès");
    }

    @Override
    public MutationResult deleteNote(DeleteNoteCommand command) {
        if (command.noteId() <= 0) {
            return MutationResult.validationError("Aucune note sélectionnée !");
        }
        var existing = noteService.getNodeNote(
                command.conceptId(), command.thesaurusId(), command.lang(), command.typeCode());
        String oldValue = existing != null ? StringUtils.defaultString(existing.getLexicalValue()) : "";
        noteService.deleteThisNote(
                command.noteId(), command.conceptId(), command.lang(), command.thesaurusId(),
                command.typeCode(), oldValue, command.userId());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Note supprimée avec succès");
    }

    @Override
    public MutationResult deprecateConcept(DeprecateConceptCommand command) {
        if (!conceptService.deprecateConcept(command.conceptId(), command.thesaurusId(), command.userId())) {
            return MutationResult.failure("Le concept n'a pas été déprécié !");
        }
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Le concept est maintenant obsolète");
    }

    @Override
    public MutationResult approveConcept(ApproveConceptCommand command) {
        if (!conceptService.approveConcept(command.conceptId(), command.thesaurusId(), command.userId())) {
            return MutationResult.failure("Le concept n'a pas été approuvé !");
        }
        if (command.addReplacedByRelations()) {
            var replacedBy = conceptService.getAllReplacedBy(
                    command.thesaurusId(), command.conceptId(), command.lang());
            if (replacedBy != null) {
                for (var replacement : replacedBy) {
                    relationService.addRelationRT(
                            command.conceptId(), command.thesaurusId(), replacement.getId(), command.userId());
                }
            }
        }
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Le concept n'est plus obsolète maintenant");
    }

    @Override
    public MutationResult addReplacedBy(AddReplacedByCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Pas de concept sélectionné !");
        }
        conceptService.addReplacedBy(
                command.conceptId(), command.thesaurusId(), command.targetConceptId(), command.userId());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Override
    public MutationResult deleteReplacedBy(DeleteReplacedByCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Pas de concept sélectionné !");
        }
        conceptService.deleteReplacedBy(command.conceptId(), command.thesaurusId(), command.targetConceptId());
        return finalizeLexicalMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    private MutationResult validateSynonymDuplicate(String thesaurusId, String lang, String value) {
        if (termService.existsPrefLabel(value, lang, thesaurusId)
                || termService.isAltLabelExist(value, thesaurusId, lang)) {
            return MutationResult.duplicate("Un label identique existe déjà !");
        }
        return null;
    }

    private MutationResult finalizeLexicalMutation(
            String thesaurusId,
            String conceptId,
            int userId,
            String contributorName,
            String successMessage
    ) {
        conceptService.updateDateOfConcept(thesaurusId, conceptId, userId);
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(contributorName)
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .build());
        return MutationResult.ok(successMessage);
    }

    @Override
    public MutationResult updateNarrowerRelationType(UpdateNarrowerRelationTypeCommand command) {
        if (StringUtils.isAnyBlank(command.targetConceptId(), command.ntRole())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        String inverseRelation = inverseNtRole(command.ntRole());
        if (!relationService.updateRelationNT(
                command.conceptId(),
                command.targetConceptId(),
                command.thesaurusId(),
                command.ntRole(),
                inverseRelation,
                command.userId())) {
            return MutationResult.failure("Erreur modifiant la relation pour le concept !");
        }
        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation modifiée avec succès");
    }

    @Override
    public MutationResult applyNarrowerRelationToBranch(ApplyNarrowerRelationToBranchCommand command) {
        if (StringUtils.isBlank(command.ntRole())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        String inverseRelation = inverseNtRole(command.ntRole());
        applyRelationToBranchRecursive(
                command.conceptId(),
                command.thesaurusId(),
                command.ntRole(),
                inverseRelation,
                command.userId());
        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation modifiée avec succès");
    }

    @Override
    public MutationResult addRelatedRelation(AddRelatedRelationCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        if (relationService.isConceptHaveRelationNTorBT(
                command.conceptId(), command.targetConceptId(), command.thesaurusId())) {
            return MutationResult.validationError("Relation non permise !");
        }
        if (!relationService.addRelationRT(
                command.conceptId(), command.thesaurusId(), command.targetConceptId(), command.userId())) {
            return MutationResult.failure("Erreur pendant l'ajout de la relation !");
        }
        if (command.tagPrefLabel()) {
            applyTagPrefLabel(command);
        }
        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Override
    public MutationResult deleteRelatedRelation(DeleteRelatedRelationCommand command) {
        MutationResult validation = validateRelationTarget(command.targetConceptId());
        if (validation != null) {
            return validation;
        }
        relationService.deleteRelationRT(
                command.conceptId(), command.thesaurusId(), command.targetConceptId(), command.userId());
        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    @Override
    public MutationResult addCustomRelation(AddCustomRelationCommand command) {
        if (StringUtils.isBlank(command.targetConceptId())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        var targetConcept = conceptService.getConcept(command.targetConceptId(), command.thesaurusId());
        if (targetConcept == null || StringUtils.isBlank(targetConcept.getConceptType())) {
            return MutationResult.validationError("Le type de concept n'est pas reconnu !");
        }
        var nodeConceptType = conceptTypeService.getNodeTypeConcept(
                targetConcept.getConceptType(), command.thesaurusId());
        if (nodeConceptType == null) {
            return MutationResult.validationError("Le type de concept n'est pas reconnu !");
        }
        relationService.addCustomRelationship(
                command.conceptId(),
                command.thesaurusId(),
                command.targetConceptId(),
                command.userId(),
                targetConcept.getConceptType(),
                nodeConceptType.isReciprocal());
        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation ajoutée avec succès");
    }

    @Override
    public MutationResult deleteCustomRelation(DeleteCustomRelationCommand command) {
        if (StringUtils.isAnyBlank(command.targetConceptId(), command.relationCode())) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        relationService.deleteCustomRelationship(
                command.conceptId(),
                command.thesaurusId(),
                command.targetConceptId(),
                command.userId(),
                command.relationCode(),
                command.reciprocal());
        return finalizeRelationMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName(),
                "Relation supprimée avec succès");
    }

    private void applyTagPrefLabel(AddRelatedRelationCommand command) {
        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(
                command.thesaurusId(), command.conceptId());
        String idTerm = preferredTerm.map(fr.cnrs.opentheso.entites.PreferredTerm::getIdTerm).orElse(null);
        if (idTerm == null || !termService.isTermExistInLangAndThesaurus(idTerm, command.thesaurusId(), command.lang())) {
            return;
        }
        String currentLabel = termService.getLexicalValueOfConcept(
                command.conceptId(), command.thesaurusId(), command.lang());
        String taggedValue = termService.getLexicalValueOfConcept(
                command.targetConceptId(), command.thesaurusId(), command.lang());
        if (StringUtils.isAnyBlank(currentLabel, taggedValue)) {
            return;
        }
        termService.updateTermTraduction(
                currentLabel + " (" + taggedValue + ")",
                idTerm,
                command.lang(),
                command.thesaurusId(),
                command.userId());
    }

    private void applyRelationToBranchRecursive(
            String parentConceptId,
            String thesaurusId,
            String relation,
            String inverseRelation,
            int userId
    ) {
        for (String childConceptId : conceptService.getListChildrenOfConcept(parentConceptId, thesaurusId)) {
            relationService.updateRelationNT(
                    parentConceptId, childConceptId, thesaurusId, relation, inverseRelation, userId);
            applyRelationToBranchRecursive(childConceptId, thesaurusId, relation, inverseRelation, userId);
        }
    }

    private String inverseNtRole(String role) {
        return switch (StringUtils.defaultString(role)) {
            case "NT" -> "BT";
            case "NTG" -> "BTG";
            case "NTP" -> "BTP";
            case "NTI" -> "BTI";
            default -> "BT";
        };
    }

    private MutationResult validateHierarchicalRelationCommand(
            String thesaurusId,
            String conceptId,
            String targetConceptId
    ) {
        if (StringUtils.isBlank(targetConceptId)) {
            return MutationResult.validationError("Aucune sélection !");
        }
        if (isHierarchicalRelationInvalid(thesaurusId, conceptId, targetConceptId)) {
            return MutationResult.validationError("Relation non permise !");
        }
        return null;
    }

    private MutationResult validateRelationTarget(String targetConceptId) {
        if (StringUtils.isBlank(targetConceptId)) {
            return MutationResult.validationError("Aucune relation n'est sélectionnée !");
        }
        return null;
    }

    private boolean isHierarchicalRelationInvalid(String thesaurusId, String conceptId, String targetConceptId) {
        return conceptId.equalsIgnoreCase(targetConceptId)
                || relationService.isConceptHaveRelationRT(conceptId, targetConceptId, thesaurusId)
                || relationService.isConceptHaveRelationNTorBT(conceptId, targetConceptId, thesaurusId);
    }

    private MutationResult finalizeRelationMutation(
            String thesaurusId,
            String conceptId,
            int userId,
            String contributorName,
            String successMessage
    ) {
        conceptService.updateDateOfConcept(thesaurusId, conceptId, userId);
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CONTRIBUTOR)
                .value(contributorName)
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .build());
        return MutationResult.ok(successMessage);
    }

    private MutationResult validateNewConceptFields(
            String thesaurusId,
            String lang,
            String preferredLabel,
            String notation,
            String customConceptId,
            boolean forcedDuplicate
    ) {
        if (StringUtils.isBlank(preferredLabel)) {
            return MutationResult.validationError("le label est obligatoire !");
        }
        String label = preferredLabel.trim();
        if (!forcedDuplicate) {
            if (termService.existsPrefLabel(label, lang, thesaurusId)) {
                return MutationResult.duplicate("un prefLabel existe déjà avec ce nom !");
            }
            if (termService.isAltLabelExist(label, thesaurusId, lang)) {
                return MutationResult.duplicate("un synonyme existe déjà avec ce nom !");
            }
        }
        if (StringUtils.isNotBlank(notation) && conceptService.isNotationExist(thesaurusId, notation.trim())) {
            return MutationResult.validationError("Notation existe déjà, veuillez choisir une autre !");
        }
        if (StringUtils.isNotBlank(customConceptId) && conceptAddService.isIdExiste(customConceptId, thesaurusId)) {
            return MutationResult.validationError("Identifiant déjà attribué !");
        }
        return null;
    }

    private boolean isSameConceptTerm(fr.cnrs.opentheso.entites.Term found, RenamePreferredLabelCommand command) {
        return preferredTermRepository.findByIdThesaurusAndIdConcept(command.thesaurusId(), command.conceptId())
                .flatMap(pt -> termRepository.findByIdTermAndIdThesaurusAndLang(
                        pt.getIdTerm(), command.thesaurusId(), command.lang()))
                .map(current -> current.getId().equals(found.getId()))
                .orElse(false);
    }

    private void saveCreatorDcTerm(String thesaurusId, String conceptId, String contributorName) {
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CREATOR)
                .value(contributorName)
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .build());
    }
}
