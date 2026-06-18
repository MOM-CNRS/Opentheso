package fr.cnrs.opentheso.services;

import fr.cnrs.opentheso.bean.language.LanguageBean;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.CandidatMessages;
import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.entites.CandidatVote;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.TraductionDto;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.NodeCandidateOld;
import fr.cnrs.opentheso.models.candidats.NodeTraductionCandidat;
import fr.cnrs.opentheso.models.candidats.NodeProposition;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.repositories.CandidatMessageRepository;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import fr.cnrs.opentheso.repositories.ConceptCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptTermCandidatRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.repositories.TermCandidatRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.ThesaurusRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost.Candidate;
import fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost.Element;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;


@Slf4j
@Service
@RequiredArgsConstructor
public class CandidatService {

    private final MailService mailBean;
    private final ImageService imageService;
    private final TermService termService;
    private final AlignmentService alignmentService;
    private final ConceptAddService conceptAddService;
    private final NoteService noteService;
    private final RelationService relationService;
    private final GroupService groupService;
    private final LanguageBean languageBean;
    private final NonPreferredTermService nonPreferredTermService;
    private final CandidatMessageRepository candidatMessageRepository;
    private final CandidatVoteRepository candidatVoteRepository;
    private final StatusRepository statusRepository;
    private final CandidatStatusRepository candidatStatusRepository;
    private final ConceptRepository conceptRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptCandidatRepository conceptCandidatRepository;
    private final ConceptTermCandidatRepository conceptTermCandidatRepository;
    private final PropositionService propositionService;
    private final UserRepository userRepository;
    private final TermCandidatRepository termCandidatRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final TermRepository termRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NoteRepository noteRepository;
    private final ThesaurusRepository thesaurusRepository;

    private final MailService mailService;


    public List<CandidatDto> getCandidatsByStatus(String idThesaurus, String lang, int stat) {

        log.debug("Rechercher la liste des candidats dans le thésaurus {} avec le status {}", idThesaurus, stat);
        var candidatList = getCandidatsByThesaurusAndStatus(idThesaurus, lang, stat);
        candidatList.forEach(candidatDto -> {
            var candidatMessages = candidatMessageRepository.findMessagesByConceptAndThesaurus(candidatDto.getIdConcepte(), idThesaurus);
            candidatDto.setNbrParticipant(CollectionUtils.isEmpty(candidatMessages) ? 0 : candidatMessages.size());

            candidatDto.setNbrDemande(propositionService.getPropositionByConceptAndThesaurus(idThesaurus, candidatDto.getIdConcepte()).size());

            var candidatVotes = candidatVoteRepository.findAllByIdConceptAndIdThesaurusAndTypeVote(candidatDto.getIdConcepte(), idThesaurus, VoteType.CANDIDAT.getLabel());
            candidatDto.setNbrVote(CollectionUtils.isEmpty(candidatVotes) ? 0 : candidatVotes.size());

            var noteVotes = candidatVoteRepository.findAllByIdConceptAndIdThesaurusAndTypeVote(candidatDto.getIdConcepte(), idThesaurus, VoteType.NOTE.getLabel());
            candidatDto.setNbrNoteVote(CollectionUtils.isEmpty(noteVotes) ? 0 : noteVotes.size());

            candidatDto.setAlignments(alignmentService.getAllAlignmentOfConcept(candidatDto.getIdConcepte(), idThesaurus));
        });
        return candidatList;
    }

    public String saveNewCandidat(Concept concept) throws SQLException {

        var idNewCandidat = conceptAddService.addConceptInTable(concept, concept.getIdUser());
        var status = statusRepository.findById(1);
        candidatStatusRepository.save(CandidatStatus.builder()
                        .idConcept(idNewCandidat)
                        .idThesaurus(concept.getIdThesaurus())
                        .idUser(concept.getIdUser())
                        .date(new Date())
                        .status(status.orElse(null))
                .build());
        return idNewCandidat;
    }

    public boolean updateCandidatStatus(String idConcept, String idThesaurus, int statusId) {
        var candidatStatus = candidatStatusRepository.findAllByIdConceptAndIdThesaurus(idConcept, idThesaurus);
        if (candidatStatus.isPresent()) {
            var newStatus = statusRepository.findById(statusId);
            if (newStatus.isPresent()) {
                candidatStatus.get().setStatus(newStatus.get());
                candidatStatusRepository.save(candidatStatus.get());
                return true;
            }
        }
        return false;
    }

    public String saveNewTerm(Term term, String idConcept, int idUser) throws SQLException {

        return termService.addTerm(term, idConcept, idUser);
    }

    public void updateIntitule(String intitule, String idThesaurus, String lang, String idTerm) {
        termService.updateIntitule(intitule, idTerm, idThesaurus, lang);
    }

    public void updateDetailsCondidat(CandidatDto candidatSelected) {

        //update domaine
        groupService.deleteAllGroupOfConcept(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus());
        
        for (NodeIdValue collection : candidatSelected.getCollections()) {
            groupService.addNewDomaine(collection.getId(), candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte());
        }

        // gestion des relations
        relationService.deleteAllRelationOfConcept(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus());

        //update terme générique
        if (!CollectionUtils.isEmpty(candidatSelected.getTermesGenerique())) {
            candidatSelected.getTermesGenerique().forEach(nodeBT ->
                    relationService.addHierarchicalRelation(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus(), "BT", nodeBT.getId()));
        }

        //update terme associés
        if (!CollectionUtils.isEmpty(candidatSelected.getTermesAssocies())) {
            candidatSelected.getTermesAssocies().forEach(nodeRT ->
                    relationService.addHierarchicalRelation(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus(), "RT", nodeRT.getId()));
        }

        // Employé pour
        nonPreferredTermService.deleteEMByIdTermAndLang(candidatSelected.getIdTerm(), candidatSelected.getIdThesaurus(), candidatSelected.getLang());
        
        if(!candidatSelected.getEmployePourList().isEmpty()) {
            candidatSelected.getEmployePourList().forEach(employe -> termService.addSynonyme(employe, candidatSelected.getIdThesaurus(), candidatSelected.getLang(),
                    candidatSelected.getIdTerm()));
        }
    }

    public void getCandidatDetails(CandidatDto candidatSelected, String idThesaurus) {

        var preferredTerm = preferredTermRepository.findByIdThesaurusAndIdConcept(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte());
        candidatSelected.setIdTerm(preferredTerm.map(PreferredTerm::getIdTerm).orElse(null));

        candidatSelected.setCollections(groupService.getDomaineCandidatByConceptAndThesaurusAndLang(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getLang()));

        candidatSelected.setTermesGenerique(relationService.getCandidatRelationsBT(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getLang()));

        candidatSelected.setTermesAssocies(relationService.getCandidatRelationsRT(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getLang()));

        candidatSelected.setEmployePourList(termService.getSynonymesParConcept(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getLang()));

        candidatSelected.setNodeNotes(noteService.getNotesCandidat(candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus()));

        candidatSelected.getNodeNotes().forEach(note -> note.setVoted(isHaveVote(candidatSelected.getIdThesaurus(),
                candidatSelected.getIdConcepte(), candidatSelected.getUserId(), note.getIdNote()+"", VoteType.NOTE)));

        var traductions = termService.getTraductionsOfConcept(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getLang());
        candidatSelected.setTraductions(traductions.stream()
                .map(element -> TraductionDto.builder()
                        .langue(element.getLang())
                        .traduction(element.getLexicalValue())
                        .codePays(element.getCodePays())
                        .build())
                .toList());

        var candidatMessages = candidatMessageRepository.findMessagesByConceptAndThesaurus(
                candidatSelected.getIdConcepte(), candidatSelected.getIdThesaurus());
        if (CollectionUtils.isNotEmpty(candidatMessages)) {
            candidatSelected.setMessages(candidatMessages.stream().map(element ->
                            MessageDto.builder()
                                    .msg(element.getValue())
                                    .nom(element.getUsername())
                                    .idUser(element.getIdUser())
                                    .mine(candidatSelected.getUserId() == element.getIdUser())
                                    .date(element.getDate())
                                    .build())
                    .toList());
        } else {
            candidatSelected.setMessages(List.of());
        }

        var votes = candidatVoteRepository.findAllByIdConceptAndIdThesaurusAndIdUserAndTypeVote(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus(), candidatSelected.getUserId(), VoteType.CANDIDAT.getLabel());
        candidatSelected.setVoted(CollectionUtils.isNotEmpty(votes));

        candidatSelected.setAlignments(alignmentService.getAllAlignmentOfConcept(candidatSelected.getIdConcepte(), idThesaurus));
        candidatSelected.setImages(imageService.getAllExternalImages(candidatSelected.getIdThesaurus(), candidatSelected.getIdConcepte()));
    }


///////// ajouté par Miled
    public void addVote(String idThesaurus, String idConcept, int idUser, String idNote, VoteType voteType) {

        candidatVoteRepository.save(CandidatVote.builder()
                .idConcept(idConcept)
                .idThesaurus(idThesaurus)
                .idUser(idUser)
                .idNote(idNote)
                .typeVote(voteType.getLabel())
                .build());
    }

    public boolean isHaveVote(String idThesaurus, String idConcept, int idUser, String idNote, VoteType voteType) {

        return CollectionUtils.isNotEmpty(candidatVoteRepository.findAllByIdConceptAndIdThesaurusAndIdUserAndIdNoteAndTypeVote(idConcept,
                idThesaurus, idUser, idNote, voteType.getLabel()));
    }

    public void removeVote(String idThesaurus, String idConcept, int idUser, String idNote, VoteType voteType) throws SQLException {

        candidatVoteRepository.deleteAllByIdUserAndIdConceptAndIdThesaurusAndTypeVoteAndIdNote(idUser, idConcept,
                idThesaurus, voteType.getLabel(), idNote);
    }
    
    public boolean insertCandidate(CandidatDto candidatDto, String adminMessage, int idUser) {

        var candidatStatus = candidatStatusRepository.findByIdConcept(candidatDto.getIdConcepte());
        if (candidatStatus.isPresent()) {
            candidatStatus.get().setStatus(statusRepository.findById(2).orElse(null));
            candidatStatus.get().setMessage(adminMessage);
            candidatStatus.get().setIdUserAdmin(idUser);
            candidatStatusRepository.save(candidatStatus.get());

            conceptRepository.setStatus("D", candidatDto.getIdConcepte(), candidatDto.getIdThesaurus());
            conceptRepository.setTopConceptTag(CollectionUtils.isEmpty(candidatDto.getTermesGenerique()),
                    candidatDto.getIdConcepte(), candidatDto.getIdThesaurus());

            return false;
        }
        return true;
    }
    
    public boolean rejectCandidate(CandidatDto candidatDto, String adminMessage, int idUser) {
        var candidatStatus = candidatStatusRepository.findByIdConcept(candidatDto.getIdConcepte());
        if (candidatStatus.isPresent()) {
            candidatStatus.get().setStatus(statusRepository.findById(3).orElse(null));
            candidatStatus.get().setMessage(adminMessage);
            candidatStatus.get().setIdUserAdmin(idUser);
            candidatStatusRepository.save(candidatStatus.get());
            return false;
        }
        return true;
    }     
    
    /**
     * permet de récupérer les anciens candidats saisies dans l'ancien module uniquement les candidats qui étatient en attente
     */    
    public String getOldCandidates(String idTheso, int idUser) {

        StringBuilder messages = new StringBuilder();

        var nodeCandidateOlds = getCandidatesIdFromOldModule(idTheso);
        if(nodeCandidateOlds == null || nodeCandidateOlds.isEmpty()) {
            return "Pas d'anciens candidats à récupérer";
        }
        
        for (NodeCandidateOld nodeCandidateOld : nodeCandidateOlds) {
            nodeCandidateOld.setNodeTraductions(getCandidatesTraductionsFromOldModule(nodeCandidateOld.getIdCandidate(), idTheso));
            var proposition = propositionService.getPropositionByConceptAndThesaurus(idTheso, nodeCandidateOld.getIdCandidate());
            if (CollectionUtils.isNotEmpty(proposition)) {
                nodeCandidateOld.setNodePropositions(proposition.stream()
                        .map(element-> NodeProposition.builder()
                                .note(element.getNote())
                                .idUser(element.getIdUser())
                                .build())
                        .toList());
            }
        }
        
        //// ajout des anciens candidats au nouveau module
        boolean exist = false;
        boolean first = true;
        Concept concept = new Concept();
        String idNewConcept = null;
        String idNewTerm = null;
        Term terme = new Term();
        
        for (NodeCandidateOld nodeCandidateOld : nodeCandidateOlds) {
            log.debug("ajout du candidat s'il n'existe pas dans le thésaurus en vérifiant langue par langue");
            for (NodeTraductionCandidat nodeTraduction : nodeCandidateOld.getNodeTraductions()) {
                log.debug("Vérification de l'existance du terme (recherche dans prefLabels)");
                if (termService.existsPrefLabel(nodeTraduction.getTitle().trim(), nodeTraduction.getIdLang(), idTheso)){
                    messages.append("Candidat existe : ").append(nodeTraduction.getTitle());
                    exist = true;
                    break;
                }
            }
            // si le candidat n'existe pas dans toutes les langues, on l'ajoute
            if(!exist) {
                concept.setIdConcept(null);
                concept.setIdThesaurus(idTheso);
                concept.setTopConcept(false);
                concept.setIdUser(idUser);
                concept.setStatus("CA");

                try {
                    idNewConcept = saveNewCandidat(concept);
                } catch (SQLException e) {
                    messages.append("Erreur : ").append(nodeCandidateOld.getIdCandidate());
                }
                if (idNewConcept == null) {
                    messages.append("Erreur : ").append(nodeCandidateOld.getIdCandidate());
                    continue;
                }

                for (NodeTraductionCandidat nodeTraduction : nodeCandidateOld.getNodeTraductions()) {
                    if(first) {
                        terme.setIdThesaurus(idTheso);
                        terme.setLang(nodeTraduction.getIdLang());
                        terme.setContributor(idUser);
                        terme.setLexicalValue(nodeTraduction.getTitle().trim());
                        terme.setSource("candidat");
                        terme.setStatus("D");
                        try {
                            idNewTerm = saveNewTerm(terme, idNewConcept, idUser);                            
                        } catch (SQLException e) {
                            messages.append("Erreur : ").append(nodeCandidateOld.getIdCandidate());
                            continue;                            
                        }
                        first = false;
                    } else {
                        var term = Term.builder()
                                .idTerm(idNewTerm)
                                .idThesaurus(idTheso)
                                .lang(nodeTraduction.getIdLang())
                                .lexicalValue(nodeTraduction.getTitle())
                                .source("candidat")
                                .status("D")
                                .build();
                        termService.addTermTraduction(term, idUser);
                    }
                }
                first = true;
                // ajout des messages  
                for (NodeProposition nodeProposition : nodeCandidateOld.getNodePropositions()) {
                    candidatMessageRepository.save(CandidatMessages.builder()
                            .value(nodeProposition.getNote())
                            .idUser(nodeProposition.getIdUser())
                            .idThesaurus(idTheso)
                            .idConcept(idNewConcept)
                            .date(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                            .build());
                }
                
                idNewConcept = null;
                idNewTerm = null;                
            }
            exist = false;
        }

        return "Import réussi\n" + messages;
    }


    //Sauvegarde un candidat en base à partir d'un JSON
    public boolean saveCandidat(Candidate candidate, int userId) {

        var idTerm = String.valueOf(conceptRepository.getNextConceptNumericId());
        var idConcept = String.valueOf(conceptRepository.getNextConceptNumericId());

        if (CollectionUtils.isNotEmpty(candidate.getTerme())) {
            for (Element term : candidate.getTerme()) {
                termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                        .idTerm(idTerm)
                        .lexicalValue(term.getValue())
                        .lang(term.getLang())
                        .idThesaurus(candidate.getThesoId())
                        .created(new Date())
                        .status("D")
                        .source("candidat")
                        .creator(userId)
                        .modified(new Date())
                        .build());
            }
        }

        log.debug("Création du nouveau concept dans la base");
        var thesaurus = thesaurusRepository.findById(candidate.getThesoId());
        if (thesaurus.isEmpty()) {
            log.error("Le thésaurus avec id {} n'existe pas dans la base de données", candidate.getThesoId());
            return false;
        }
        conceptRepository.save(fr.cnrs.opentheso.entites.Concept.builder()
                .idConcept(idConcept)
                .idThesaurus(thesaurus.get().getIdThesaurus())
                .topConcept(false)
                .gps(false)
                .conceptType("concept")
                .status("CA")
                .idArk("")
                .idHandle("")
                .idDoi("")
                .notation("")
                .creator(userId)
                .created(new Date())
                .modified(new Date())
                .contributor(userId)
                .build());

        if(candidate.getCollectionId() != null && candidate.getCollectionId().length() > 0) {
            conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                    .idGroup(candidate.getCollectionId())
                    .idThesaurus(candidate.getThesoId())
                    .idConcept(idConcept)
                    .build());
        }

        if (CollectionUtils.isNotEmpty(candidate.getDefinition())) {
            for (Element definition : candidate.getDefinition()) {
                insertNoteInCandidat("definition", candidate.getThesoId(), idTerm, definition.getLang(),
                        definition.getValue(), userId, candidate.getSource(), idConcept);
            }
        }

        if (CollectionUtils.isNotEmpty(candidate.getNote())) {
            for (Element note : candidate.getNote()) {
                insertNoteInCandidat("note", candidate.getThesoId(), idTerm, note.getLang(),
                        note.getValue(), userId, candidate.getSource(), idConcept);
            }
        }

        if (StringUtils.isNotEmpty(candidate.getComment())) {
            candidatMessageRepository.save(CandidatMessages.builder()
                    .value(candidate.getComment())
                    .idUser(userId)
                    .date(new SimpleDateFormat("yyyy-MM-dd mm:HH").format(new Date()))
                    .idConcept(idConcept)
                    .idThesaurus(candidate.getThesoId())
                    .build());
        }

        if (CollectionUtils.isNotEmpty(candidate.getSynonymes())) {
            for (Element synonyme : candidate.getSynonymes()) {
                nonPreferredTermRepository.save(NonPreferredTerm.builder()
                        .lexicalValue(synonyme.getValue())
                        .lang(synonyme.getLang())
                        .idThesaurus(candidate.getThesoId())
                        .idTerm(idTerm)
                        .created(new Date())
                        .modified(new Date())
                        .build());
            }
        }

        if (StringUtils.isNotEmpty(candidate.getConceptGenericId())) {
            relationService.addHierarchicalRelation(idConcept, candidate.getThesoId(), "BT", candidate.getConceptGenericId());
        }

        preferredTermRepository.save(PreferredTerm.builder()
                .idConcept(idConcept)
                .idTerm(idTerm)
                .idThesaurus(candidate.getThesoId())
                .build());

        var cadiadatStatus = statusRepository.findById(1);
        cadiadatStatus.ifPresent(status -> candidatStatusRepository.save(CandidatStatus.builder()
                .idConcept(idConcept)
                .status(status)
                .date(new Date(System.currentTimeMillis()))
                .idUser(userId)
                .idThesaurus(candidate.getThesoId())
                .build()));

        return true;
    }

    private void insertNoteInCandidat(String type, String thesoId, String idTerm, String lang, String description,
                                      int userId, String source, String idConcept) {

        noteRepository.save(Note.builder()
                .noteTypeCode(type)
                .idThesaurus(thesoId)
                .idTerm(idTerm)
                .lang(lang)
                .lexicalValue(description)
                .created(new Date())
                .modified(new Date())
                .idUser(userId)
                .noteSource(source)
                .idConcept(idConcept)
                .build());
    }

    public void deleteVoteByNoteId(int idNote, String idThesaurus, String idConcept) {

        log.debug("Suppression des votes par note id {}", idNote);
        candidatVoteRepository.deleteAllByIdThesaurusAndIdConceptAndIdNote(idThesaurus, idConcept, String.valueOf(idNote));
    }

    public void deleteAllCandidatsByThesaurus(String idThesaurus) {

        log.debug("Suppression de tous les votes des candidats dans le thésaurus id {}", idThesaurus);
        candidatVoteRepository.deleteAllByIdThesaurus(idThesaurus);

        log.debug("Suppression de tous les status des candidats dans le thésaurus id {}", idThesaurus);
        candidatStatusRepository.deleteAllByIdThesaurus(idThesaurus);

        log.debug("Suppression de tous les messages des candidats dans le thésaurus id {}", idThesaurus);
        candidatMessageRepository.deleteAllByIdThesaurus(idThesaurus);

        log.debug("Suppression des concept candidats dans le thésaurus id {}", idThesaurus);
        conceptCandidatRepository.deleteAllByIdThesaurus(idThesaurus);

        log.debug("Suppression des concepts terms des candidats dans le thésaurus id {}", idThesaurus);
        conceptTermCandidatRepository.deleteAllByIdThesaurus(idThesaurus);

        log.debug("Suppression des terms des candidats dans le thésaurus id {}", idThesaurus);
        termCandidatRepository.deleteAllByIdThesaurus(idThesaurus);
    }

    public void updateThesaurusId(String oldIdThesaurus, String newIdThesaurus) {

        log.debug("Mise à jour de l'id thésaurus dans le module candidat du {} vers {}", oldIdThesaurus, newIdThesaurus);
        log.debug("Mise à jour de tous les votes des candidats dans le thésaurus id {}", oldIdThesaurus);
        candidatVoteRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Mise à jour de tous les status des candidats dans le thésaurus id {}", oldIdThesaurus);
        candidatStatusRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Mise à jour de tous les messages des candidats dans le thésaurus id {}", oldIdThesaurus);
        candidatMessageRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Mise à jour des concept candidats dans le thésaurus id {}", oldIdThesaurus);
        conceptCandidatRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Mise à jour des concepts terms des candidats dans le thésaurus id {}", oldIdThesaurus);
        conceptTermCandidatRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);

        log.debug("Mise à jour des terms des candidats dans le thésaurus id {}", oldIdThesaurus);
        termCandidatRepository.updateThesaurusId(newIdThesaurus, oldIdThesaurus);
    }

    private List<NodeCandidateOld> getCandidatesIdFromOldModule(String idThesaurus) {

        log.debug("Recherche des candidats présent dans le thésaurus id {}", idThesaurus);
        var candidats = conceptCandidatRepository.findAllByIdThesaurusAndStatus(idThesaurus, "a");
        if (CollectionUtils.isEmpty(candidats)) {
            log.debug("Aucun candidat n'est trouvé dans le thésaurus id {} avec le status 'a'", idThesaurus);
        }

        log.debug("{} cadidats sont trouvés dans le thésaurus id {}", candidats.size(), idThesaurus);
        return candidats.stream()
                .map(candidat -> NodeCandidateOld.builder()
                        .idCandidate(candidat.getIdConcept())
                        .status(candidat.getStatus())
                        .build())
                .toList();
    }

    private List<CandidatDto> getCandidatsByThesaurusAndStatus(String idThesaurus, String lang, int stat) {

        log.debug("Chargement des candidats en statut {} pour le thésaurus {}", stat, idThesaurus);
        var projections = candidatStatusRepository.findCandidatesByStatus(idThesaurus, stat);
        if (CollectionUtils.isEmpty(projections)) {
            log.debug("Aucun candidat n'est trouvé dans le thésaurus id {}", idThesaurus);
            return List.of();
        }

        log.debug("{} candidats trouvés dans le thésaurus id {} avec l'état {}", projections.size(), idThesaurus, stat);
        return projections.stream()
                .map(projection ->
                    CandidatDto.builder()
                        .idConcepte(projection.getIdConcept())
                        .creationDate(projection.getCreated())
                        .insertionDate(projection.getModified())
                        .statut(String.valueOf(stat))
                        .createdById(projection.getIdUser())
                        .createdByIdAdmin(projection.getIdUserAdmin() == null ? -1 : projection.getIdUserAdmin())
                        .idThesaurus(idThesaurus)
                        .adminMessage(projection.getMessage())
                        .nomPref(termService.getLexicalValueOfConcept(projection.getIdConcept(), idThesaurus, lang))
                        .createdBy(userRepository.findById(projection.getIdUser()).map(User::getUsername).orElse("Utilisateur inconnu"))
                        .createdByAdmin(projection.getIdUserAdmin() != null ?
                                userRepository.findById(projection.getIdUserAdmin()).map(User::getUsername).orElse("Utilisateur inconnu") : "Utilisateur inconnu")
                        .build())
                .toList();
    }

    private List<CandidatDto> searchCandidatsByValue(String value, String idThesaurus, String lang, int etat, String statut) {

        log.debug("Recherche de candidats avec la valeur '{}' pour le thésaurus '{}', langue '{}', statut '{}', état {}",
                value, idThesaurus, lang, statut, etat);
        var results = conceptCandidatRepository.searchCandidatesByValue(value, idThesaurus, lang, etat, statut);
        if (CollectionUtils.isEmpty(results)) {
            log.debug("Aucun candidat n'est trouvé avec la valeur {}", value);
            return List.of();
        }

        log.debug("{} candidats est trouvé avec la valeur {}", results.size(), value);
        return results.stream()
                .map(projection ->
                        CandidatDto.builder()
                                .idTerm(projection.getIdTerm())
                                .nomPref(projection.getLexicalValue())
                                .idConcepte(projection.getIdConcept())
                                .idThesaurus(projection.getIdThesaurus())
                                .creationDate(projection.getCreated())
                                .user(projection.getUsername())
                                .userId(projection.getContributor())
                                .build())
                .toList();
    }

    private List<NodeTraductionCandidat> getCandidatesTraductionsFromOldModule(String idOldCandidat, String idThesaurus) {

        log.debug("Récupération des traductions du candidat '{}' pour le thésaurus '{}'", idOldCandidat, idThesaurus);
        return conceptTermCandidatRepository.getCandidateTranslations(idOldCandidat, idThesaurus).stream()
                .map(proj ->
                    NodeTraductionCandidat.builder()
                            .idLang(proj.getLang())
                            .title(proj.getLang())
                            .build())
                .toList();
    }

    public void sendMessage(CandidatDto candidatSelected, String message, Integer userId) {

        candidatMessageRepository.save(CandidatMessages.builder()
                .value(message)
                .idConcept(candidatSelected.getIdConcepte())
                .idThesaurus(candidatSelected.getIdThesaurus())
                .idUser(userId)
                .date(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()))
                .build());

        new Thread(() -> sendNotificationMail(candidatSelected)).start();
    }

    private void sendNotificationMail(CandidatDto candidatSelected) {

        // Envoi de mail aux participants à la discussion
        var subject = "Nouveau message module candidat";
        var message = "Vous avez participé à la discussion pour ce candidat "
                + candidatSelected.getNomPref() + ", "
                + " id= " + candidatSelected.getIdConcepte()
                + ". Sachez qu’un nouveau message a été posté.";

        var nodeUsers = setListUsersForMail(candidatSelected);

        if (CollectionUtils.isNotEmpty(nodeUsers)) {
            nodeUsers.stream()
                    .filter(user -> user != null && user.isAlertMail()) // Vérifie si l'alerte est activée
                    .forEach(user -> mailBean.sendMail(user.getMail(), subject, message));
        }
    }

    public List<NodeUser> setListUsersForMail(CandidatDto candidatSelected){

        if (candidatSelected != null) {
            var candidatMessages = candidatMessageRepository.findMessagesByConceptAndThesaurus(candidatSelected.getIdConcepte(),
                    candidatSelected.getIdThesaurus());
            if (CollectionUtils.isNotEmpty(candidatMessages)) {
                return candidatMessages.stream()
                        .map(element -> NodeUser.builder().idUser(element.getIdUser()).build())
                        .toList();
            } else {
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }

    public List<MessageDto> getAllMessagesByCandidat(CandidatDto candidatSelected, int idUser) {

        log.debug("Recherche des messages pour le candidat id {}", candidatSelected.getIdConcepte());
        var candidatMessages = candidatMessageRepository.findMessagesByConceptAndThesaurus(candidatSelected.getIdConcepte(),
                candidatSelected.getIdThesaurus());

        return CollectionUtils.isNotEmpty(candidatMessages)
            ? candidatMessages.stream()
                .map(element ->
                        MessageDto.builder()
                                .msg(element.getValue())
                                .nom(element.getUsername())
                                .idUser(element.getIdUser())
                                .mine(idUser == element.getIdUser())
                                .date(element.getDate())
                                .build())
                .toList()
            : List.of();
    }

    public void sendMailInvitation(String email) {
        try {
            mailService.sendMail(email, "Invitation à une conversation !", "C'est le body du message");
            MessageUtils.showInformationMessage(languageBean.getMsg("candidat.send_message.msg5"));
        } catch (Exception mex) {
            MessageUtils.showWarnMessage(languageBean.getMsg(mex.getMessage() + "candidat.send_message.msg6"));
        }
   /*     try {
            var properties = System.getProperties();
            var props = mailBean.getPrefMail();
            var message = new MimeMessage(Session.getDefaultInstance(properties));
            message.setFrom(new InternetAddress(props.getProperty("mailFrom")));

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject("Invitation à une conversation !");
            message.setText("C'est le body du message");
            Transport.send(message);
            MessageUtils.showInformationMessage(languageBean.getMsg("candidat.send_message.msg5"));
        } catch (MessagingException mex) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.send_message.msg6"));
        }*/
    }

    public boolean saveNewCandidat(CandidatDto candidatSelected, String idThesaurus, String idLang, Integer idUser,
                                String userName, String currentLang, String definition) throws SQLException {

        log.debug("Vérification de l'existance du term (recherche dans prefLabels)");
        if (termService.existsPrefLabel(candidatSelected.getNomPref().trim(), idLang, idThesaurus)) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg3"));
            return false;
        }

        log.debug("Vérification de l'existance du term (recherche dans altLabels)");
        if (termService.isAltLabelExist(candidatSelected.getNomPref().trim(), idThesaurus, idLang)) {
            MessageUtils.showWarnMessage(languageBean.getMsg("candidat.save.msg4"));
            return false;
        }

        var idNewConcept = saveNewCandidat(Concept.builder()
                .idConcept(candidatSelected.getIdConcepte())
                .idThesaurus(idThesaurus)
                .topConcept(false)
                .lang(idLang)
                .idUser(idUser)
                .userName(userName)
                .status("CA")
                .creator(idUser)
                .build());

        if (idNewConcept == null) {
            MessageUtils.showErrorMessage(languageBean.getMsg("candidat.save.msg5"));
            return false;
        }
        candidatSelected.setIdConcepte(idNewConcept);

        var terme = Term.builder()
                .lang(idLang)
                .idThesaurus(idThesaurus)
                .contributor(idUser)
                .lexicalValue(candidatSelected.getNomPref().trim())
                .source("candidat")
                .status("D")
                .created(new Date())
                .modified(new Date())
                .build();
        candidatSelected.setIdTerm(saveNewTerm(terme, candidatSelected.getIdConcepte(), candidatSelected.getUserId()));

        noteService.addNote(candidatSelected.getIdConcepte(), currentLang, idThesaurus,
                definition, "definition", "", idUser);
        return true;
    }
}
