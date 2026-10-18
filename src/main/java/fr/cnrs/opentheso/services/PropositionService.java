package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.entites.PropositionModification;
import fr.cnrs.opentheso.entites.PropositionModificationDetail;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.propositions.NotePropBean;
import fr.cnrs.opentheso.models.propositions.PropositionActionEnum;
import fr.cnrs.opentheso.models.propositions.PropositionCategoryEnum;
import fr.cnrs.opentheso.models.propositions.PropositionFromApi;
import fr.cnrs.opentheso.models.propositions.PropositionStatusEnum;
import fr.cnrs.opentheso.models.propositions.SynonymPropBean;
import fr.cnrs.opentheso.models.propositions.TraductionPropBean;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationDetailRepository;
import fr.cnrs.opentheso.repositories.PropositionModificationRepository;
import fr.cnrs.opentheso.repositories.PropositionRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;


@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class PropositionService {

    private final PropositionModificationRepository propositionModificationRepository;
    private final PropositionModificationDetailRepository propositionModificationDetailRepository;
    private final PreferenceService preferenceService;
    private final PreferredTermRepository preferredTermRepository;
    private final PropositionRepository propositionRepository;

    public List<fr.cnrs.opentheso.entites.Proposition> getPropositionByConceptAndThesaurus(String idThesaurus, String idConcept) {

        log.debug("Recherche des propositions pour le concept id {} ({})", idConcept, idThesaurus);
        var propositions = propositionRepository.findAllByIdConceptAndIdThesaurusOrderByCreated(idThesaurus, idConcept);
        if (CollectionUtils.isEmpty(propositions)) {
            log.debug("Aucune proposition n'est trouvée pour le concept id {}", idConcept);
            return List.of();
        }
        return propositions;
    }

    public void createProposition(PropositionFromApi proposition, User user) {

        var thesaurusLang = preferenceService.getWorkLanguageOfThesaurus(proposition.getIdTheso());

        int propositionId = propositionModificationRepository.save(PropositionModification.builder()
                        .nom(user.getUsername())
                        .email(user.getMail())
                        .commentaire(proposition.getCommentaire())
                        .date(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()))
                        .idConcept(proposition.getConceptID())
                        .idTheso(proposition.getIdTheso())
                        .lang(thesaurusLang)
                        .status(PropositionStatusEnum.ENVOYER.name())
                        .build())
                .getId();

        saveTerme(proposition, propositionId, thesaurusLang);

        if (CollectionUtils.isNotEmpty(proposition.getTraductionsProp())) {
            saveTraductions(proposition, propositionId, thesaurusLang);
        }

        if (!CollectionUtils.isEmpty(proposition.getSynonymsProp())) {
            saveSynonymes(proposition, propositionId);
        }

        if (CollectionUtils.isNotEmpty(proposition.getNotes())) {
            for (NotePropBean note : proposition.getNotes()) {
                noteManagement(propositionId, note, PropositionCategoryEnum.NOTE.name());
            }
        }

        if (CollectionUtils.isNotEmpty(proposition.getDefinitions())) {
            for (NotePropBean definition : proposition.getDefinitions()) {
                noteManagement(propositionId, definition, PropositionCategoryEnum.DEFINITION.name());
            }
        }
    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Mise à jour du thésaurus id pour les propositions présentes dans le thésaurus id {}", oldIdThesaurus);
        propositionRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
        propositionModificationRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    public void deleteByThesaurus(String idThesaurus) {

        log.debug("Suppression des propositions présentes dans le thésaurus id {}", idThesaurus);
        propositionModificationDetailRepository.deleteByIdThesaurus(idThesaurus);
        propositionModificationRepository.deleteAllByIdTheso(idThesaurus);
        propositionRepository.deleteAllByIdThesaurus(idThesaurus);
    }

    private void saveTerme(PropositionFromApi proposition,  Integer propositionId, String thesaurusLang) {
        var prefLabel = proposition.getTraductionsProp().stream()
                .filter(element -> element.getLang().equals(thesaurusLang))
                .findFirst();
        prefLabel.ifPresent(traductionPropBean -> propositionModificationDetailRepository.save(PropositionModificationDetail.builder()
                .idProposition(propositionId)
                .action(PropositionActionEnum.UPDATE.name())
                .categorie(PropositionCategoryEnum.NOM.name())
                .lang(traductionPropBean.getLang())
                .value(traductionPropBean.getLexicalValue())
                .oldValue(traductionPropBean.getOldValue())
                .build()));
    }

    private void saveTraductions(PropositionFromApi proposition, Integer propositionId,  String thesaurusLang) {

        var traduction = proposition.getTraductionsProp().stream().filter(element -> !element.getLang().equals(thesaurusLang)).findFirst();

        if (traduction.isPresent()) {
            if (traduction.get().isToAdd() || traduction.get().isToUpdate() || traduction.get().isToRemove()) {
                var propositionDetail = new PropositionModificationDetail();
                if (traduction.get().isToAdd()) {
                    propositionDetail.setAction(PropositionActionEnum.ADD.name());
                    propositionDetail.setOldValue(traduction.get().getLexicalValue());
                } else if (traduction.get().isToRemove()) {
                    propositionDetail.setAction(PropositionActionEnum.DELETE.name());
                    propositionDetail.setOldValue(traduction.get().getOldValue());
                } else {
                    propositionDetail.setAction(PropositionActionEnum.UPDATE.name());
                    propositionDetail.setOldValue(traduction.get().getOldValue());
                }

                var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(proposition.getIdTheso(), proposition.getConceptID());
                if (preferredTerm.isPresent()) {
                    propositionDetail.setIdProposition(propositionId);
                    propositionDetail.setCategorie(PropositionCategoryEnum.TRADUCTION.name());
                    propositionDetail.setLang(traduction.get().getLang());
                    propositionDetail.setValue(traduction.get().getLexicalValue());
                    propositionDetail.setIdTerm(preferredTerm.get().getIdTerm());
                    propositionModificationDetailRepository.save(propositionDetail);
                }
            }
        }
    }

    private void saveSynonyms(Integer propositionId, SynonymPropBean synonymProp) {
        if (synonymProp.isToAdd() || synonymProp.isToUpdate() || synonymProp.isToRemove()) {
            PropositionActionEnum action;
            if (synonymProp.isToAdd()) {
                action = PropositionActionEnum.ADD;
            } else if (synonymProp.isToRemove()) {
                action = PropositionActionEnum.DELETE;
            } else {
                action = PropositionActionEnum.UPDATE;
            }

            propositionModificationDetailRepository.save(PropositionModificationDetail.builder()
                    .idProposition(propositionId)
                    .action(action.name())
                    .categorie(PropositionCategoryEnum.SYNONYME.name())
                    .lang(synonymProp.getLang())
                    .value(synonymProp.getLexicalValue())
                    .oldValue(synonymProp.getOldValue())
                    .status(synonymProp.getStatus())
                    .hiden(synonymProp.isHiden())
                    .idTerm(synonymProp.getIdTerm())
                    .build());
        }
    }

    private void saveSynonymes(PropositionFromApi proposition, Integer propositionId) {
        for (SynonymPropBean synonymProp : proposition.getSynonymsProp()) {
            saveSynonyms(propositionId, synonymProp);
        }
    }

    private void noteManagement(int propositionID, NotePropBean note, String category) {
        if (note.isToAdd() || note.isToUpdate() || note.isToRemove()) {
            var propositionDetail = new PropositionModificationDetail();
            if (note.isToAdd()) {
                propositionDetail.setAction(PropositionActionEnum.ADD.name());
                propositionDetail.setOldValue(note.getLexicalValue());
            } else if (note.isToRemove()) {
                propositionDetail.setAction(PropositionActionEnum.DELETE.name());
                propositionDetail.setOldValue(note.getOldValue());
            } else {
                propositionDetail.setAction(PropositionActionEnum.UPDATE.name());
                propositionDetail.setOldValue(note.getOldValue());
            }

            propositionDetail.setIdProposition(propositionID);
            propositionDetail.setCategorie(category);
            propositionDetail.setLang(note.getLang());
            propositionDetail.setValue(note.getLexicalValue());
            propositionDetail.setIdTerm(note.getIdTerm());
            propositionModificationDetailRepository.save(propositionDetail);
        }
    }

}
