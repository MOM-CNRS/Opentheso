package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.concept.write.model.MutationResult;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.AddTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.DeleteTranslationCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateSynonymCommand;
import fr.cnrs.opentheso.v2.concept.write.model.command.UpdateTranslationCommand;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConceptLexicalNativeWriteService {

    private final ConceptLexicalWriteRepository conceptLexicalWriteRepository;
    private final ConceptSynonymWriteRepository conceptSynonymWriteRepository;
    private final ConceptTranslationWriteRepository conceptTranslationWriteRepository;
    private final ConceptWritePostMutationRepository conceptWritePostMutationRepository;

    @Transactional
    public MutationResult addSynonym(AddSynonymCommand command) {
        if (StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("La valeur est obligatoire !");
        }
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("Pas de langue choisie !");
        }
        if (!command.forced()) {
            MutationResult duplicate = validateSynonymDuplicate(
                    command.thesaurusId(), command.lang(), command.value());
            if (duplicate != null) {
                return duplicate;
            }
        }
        var idTerm = requirePreferredTermId(command.thesaurusId(), command.conceptId());
        if (idTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        conceptSynonymWriteRepository.insertSynonym(
                idTerm, command.thesaurusId(), command.lang(), command.value(), command.hidden(), command.userId());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Synonyme ajouté avec succès");
    }

    @Transactional
    public MutationResult updateSynonym(UpdateSynonymCommand command) {
        var idTerm = requirePreferredTermId(command.thesaurusId(), command.conceptId());
        if (idTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        if (!StringUtils.equals(command.oldValue(), command.newValue())) {
            if (!command.forced()) {
                MutationResult duplicate = validateSynonymDuplicate(
                        command.thesaurusId(), command.lang(), command.newValue());
                if (duplicate != null) {
                    return duplicate;
                }
            }
            if (!conceptSynonymWriteRepository.updateSynonym(
                    idTerm, command.thesaurusId(), command.lang(),
                    command.oldValue(), command.newValue(), command.hidden(), command.userId())) {
                return MutationResult.failure("La modification a échoué !");
            }
        } else if (!conceptSynonymWriteRepository.updateSynonymHidden(
                idTerm, command.thesaurusId(), command.lang(),
                command.newValue(), command.hidden(), command.userId())) {
            return MutationResult.failure("La modification a échoué !");
        }
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Synonyme modifié avec succès");
    }

    @Transactional
    public MutationResult deleteSynonym(DeleteSynonymCommand command) {
        if (StringUtils.isAnyBlank(command.lang(), command.value())) {
            return MutationResult.validationError("Aucune sélection !");
        }
        var idTerm = requirePreferredTermId(command.thesaurusId(), command.conceptId());
        if (idTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        conceptSynonymWriteRepository.deleteSynonym(
                idTerm, command.thesaurusId(), command.lang(), command.value(), command.userId());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Synonyme supprimé avec succès");
    }

    @Transactional
    public MutationResult addTranslation(AddTranslationCommand command) {
        if (StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("La valeur est obligatoire !");
        }
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("Aucune langue sélectionnée !");
        }
        String convertedValue = fr.cnrs.opentheso.utils.StringUtils.convertString(command.value());
        if (conceptLexicalWriteRepository.existsTermIgnoreCase(
                convertedValue, command.lang(), command.thesaurusId())) {
            return MutationResult.validationError("Un label identique existe dans cette langue !");
        }
        var idTerm = requirePreferredTermId(command.thesaurusId(), command.conceptId());
        if (idTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        conceptTranslationWriteRepository.insertTranslation(
                idTerm, command.thesaurusId(), command.lang(), convertedValue, command.userId());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Traduction ajoutée avec succès");
    }

    @Transactional
    public MutationResult updateTranslation(UpdateTranslationCommand command) {
        if (StringUtils.isBlank(command.value())) {
            return MutationResult.validationError("Veuillez saisir une valeur !");
        }
        String convertedValue = fr.cnrs.opentheso.utils.StringUtils.convertString(command.value());
        if (conceptLexicalWriteRepository.existsTermIgnoreCase(
                convertedValue, command.lang(), command.thesaurusId())) {
            return MutationResult.validationError("Un label identique existe dans cette langue !");
        }
        var idTerm = requirePreferredTermId(command.thesaurusId(), command.conceptId());
        if (idTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        conceptTranslationWriteRepository.updateTranslation(
                idTerm, command.thesaurusId(), command.lang(), convertedValue, command.userId());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Traduction modifiée avec succès");
    }

    @Transactional
    public MutationResult deleteTranslation(DeleteTranslationCommand command) {
        if (StringUtils.isBlank(command.lang())) {
            return MutationResult.validationError("Erreur de sélection de traduction !");
        }
        var idTerm = requirePreferredTermId(command.thesaurusId(), command.conceptId());
        if (idTerm == null) {
            return MutationResult.failure("Erreur de cohérence de BDD !");
        }
        conceptTranslationWriteRepository.deleteTranslation(idTerm, command.thesaurusId(), command.lang());
        finalizeMutation(command.thesaurusId(), command.conceptId(), command.userId(), command.contributorName());
        return MutationResult.ok("Traduction supprimée avec succès");
    }

    private MutationResult validateSynonymDuplicate(String thesaurusId, String lang, String value) {
        if (conceptLexicalWriteRepository.existsPrefLabel(value, lang, thesaurusId)
                || conceptLexicalWriteRepository.existsAltLabel(value, lang, thesaurusId)) {
            return MutationResult.duplicate("Un label identique existe déjà !");
        }
        return null;
    }

    private String requirePreferredTermId(String thesaurusId, String conceptId) {
        return conceptLexicalWriteRepository.findPreferredTermId(thesaurusId, conceptId).orElse(null);
    }

    private void finalizeMutation(String thesaurusId, String conceptId, int userId, String contributorName) {
        conceptWritePostMutationRepository.touchConcept(thesaurusId, conceptId, userId);
        conceptWritePostMutationRepository.saveContributorDcTerm(
                thesaurusId, conceptId, StringUtils.defaultString(contributorName));
    }
}
