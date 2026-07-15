package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.entites.CandidatMessages;
import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.entites.CandidatVote;
import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.entites.NonPreferredTerm;
import fr.cnrs.opentheso.entites.Note;
import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.entites.PreferredTerm;
import fr.cnrs.opentheso.entites.TermHistorique;
import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.NodeCandidateOld;
import fr.cnrs.opentheso.models.candidats.NodeProposition;
import fr.cnrs.opentheso.models.candidats.NodeTraductionCandidat;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.concept.Concept;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.repositories.AlignementRepository;
import fr.cnrs.opentheso.repositories.AlignementTypeRepository;
import fr.cnrs.opentheso.repositories.CandidatMessageRepository;
import fr.cnrs.opentheso.repositories.CandidatStatusRepository;
import fr.cnrs.opentheso.repositories.CandidatVoteRepository;
import fr.cnrs.opentheso.repositories.ConceptCandidatRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptGroupLabelRepository;
import fr.cnrs.opentheso.repositories.ConceptHistoriqueRepository;
import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.repositories.ConceptTermCandidatRepository;
import fr.cnrs.opentheso.repositories.HierarchicalRelationshipRepository;
import fr.cnrs.opentheso.repositories.NonPreferredTermRepository;
import fr.cnrs.opentheso.repositories.NoteRepository;
import fr.cnrs.opentheso.repositories.PreferredTermRepository;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.repositories.PropositionRepository;
import fr.cnrs.opentheso.repositories.SearchRepository;
import fr.cnrs.opentheso.repositories.StatusRepository;
import fr.cnrs.opentheso.repositories.TermHistoriqueRepository;
import fr.cnrs.opentheso.repositories.TermRepository;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.utils.ToolsHelper;
import fr.cnrs.opentheso.v2.concept.write.persistence.ConceptDeletionWriteRepository;
import fr.cnrs.opentheso.v2.toolbox.persistence.ToolboxThesaurusPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidatMutationPersistence {

    private final ConceptDeletionWriteRepository conceptDeletionWriteRepository;
    private final ToolboxThesaurusPersistence toolboxThesaurusPersistence;
    private final ConceptRepository conceptRepository;
    private final ConceptHistoriqueRepository conceptHistoriqueRepository;
    private final PreferencesRepository preferencesRepository;
    private final CandidatStatusRepository candidatStatusRepository;
    private final StatusRepository statusRepository;
    private final CandidatVoteRepository candidatVoteRepository;
    private final TermRepository termRepository;
    private final TermHistoriqueRepository termHistoriqueRepository;
    private final PreferredTermRepository preferredTermRepository;
    private final NonPreferredTermRepository nonPreferredTermRepository;
    private final NoteRepository noteRepository;
    private final AlignementRepository alignementRepository;
    private final AlignementTypeRepository alignementTypeRepository;
    private final ConceptGroupConceptRepository conceptGroupConceptRepository;
    private final ConceptGroupLabelRepository conceptGroupLabelRepository;
    private final HierarchicalRelationshipRepository hierarchicalRelationshipRepository;
    private final SearchRepository searchRepository;
    private final UserRepository userRepository;
    private final CandidatMessageRepository candidatMessageRepository;
    private final ConceptCandidatRepository conceptCandidatRepository;
    private final ConceptTermCandidatRepository conceptTermCandidatRepository;
    private final PropositionRepository propositionRepository;
    private final CandidatReadPersistence candidatReadPersistence;

    public boolean deleteConcept(String conceptId, String thesaurusId) {
        try {
            conceptDeletionWriteRepository.deleteConcept(thesaurusId, conceptId);
            return true;
        } catch (Exception ex) {
            log.error("Erreur pendant la suppression du candidat {}", conceptId, ex);
            return false;
        }
    }

    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String lang) {
        return toolboxThesaurusPersistence.loadUsedLanguages(thesaurusId, lang);
    }

    public boolean deleteAlignment(int alignmentId, String thesaurusId) {
        try {
            return alignementRepository.deleteByIdAndThesaurus(alignmentId, thesaurusId) > 0;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de l'alignement {}", alignmentId, e);
            return false;
        }
    }

    public List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId) {
        return candidatReadPersistence.loadAlignments(conceptId, thesaurusId);
    }

    public void updateAlignment(AlignementElement element, String conceptId, String thesaurusId) {
        var alignement = alignementRepository.findById(element.getIdAlignment());
        if (alignement.isEmpty()) {
            return;
        }
        var alignementType = alignementTypeRepository.findById(element.getAlignement_id_type());
        if (alignementType.isEmpty()) {
            return;
        }
        var entity = alignement.get();
        entity.setConceptTarget(fr.cnrs.opentheso.utils.StringUtils.convertString(element.getConceptTarget()));
        entity.setThesaurusTarget(element.getThesaurus_target());
        entity.setUriTarget(fr.cnrs.opentheso.utils.StringUtils.convertString(element.getTargetUri()));
        entity.setAlignementType(alignementType.get());
        entity.setModified(new Date());
        alignementRepository.save(entity);
    }

    public boolean saveNewCandidat(
            CandidatDto candidat,
            String thesaurusId,
            String lang,
            int userId,
            String username,
            String thesaurusLang,
            String definition
    ) throws SQLException, IOException {
        if (termRepository.existsPrefLabel(candidat.getNomPref().trim(), lang, thesaurusId)) {
            MessageUtils.showWarnMessage("Ce libellé préféré existe déjà");
            return false;
        }
        if (nonPreferredTermRepository.isAltLabelExist(candidat.getNomPref().trim(), thesaurusId, lang)) {
            MessageUtils.showWarnMessage("Ce libellé existe déjà comme synonyme");
            return false;
        }

        var idNewConcept = createCandidateConcept(Concept.builder()
                .idConcept(candidat.getIdConcepte())
                .idThesaurus(thesaurusId)
                .topConcept(false)
                .lang(lang)
                .idUser(userId)
                .userName(username)
                .status("CA")
                .creator(userId)
                .build());
        if (idNewConcept == null) {
            MessageUtils.showErrorMessage("Erreur pendant la création du candidat");
            return false;
        }
        candidat.setIdConcepte(idNewConcept);

        var termId = addTerm(Term.builder()
                .lang(lang)
                .idThesaurus(thesaurusId)
                .contributor(userId)
                .lexicalValue(candidat.getNomPref().trim())
                .source("candidat")
                .status("D")
                .created(new Date())
                .modified(new Date())
                .build(), idNewConcept, userId);
        candidat.setIdTerm(termId);
        addNote(idNewConcept, thesaurusLang, thesaurusId, definition, "definition", "", userId);
        return true;
    }

    public void updateCandidateDetails(CandidatDto candidat) {
        conceptGroupConceptRepository.deleteAllByIdThesaurusAndIdConcept(
                candidat.getIdThesaurus(), candidat.getIdConcepte());
        for (NodeIdValue collection : candidat.getCollections()) {
            addCollection(collection.getId(), candidat.getIdThesaurus(), candidat.getIdConcepte());
        }
        deleteAllRelations(candidat.getIdConcepte(), candidat.getIdThesaurus());
        if (!CollectionUtils.isEmpty(candidat.getTermesGenerique())) {
            candidat.getTermesGenerique().forEach(nodeBT ->
                    addHierarchicalRelation(candidat.getIdConcepte(), candidat.getIdThesaurus(), "BT", nodeBT.getId()));
        }
        if (!CollectionUtils.isEmpty(candidat.getTermesAssocies())) {
            candidat.getTermesAssocies().forEach(nodeRT ->
                    addHierarchicalRelation(candidat.getIdConcepte(), candidat.getIdThesaurus(), "RT", nodeRT.getId()));
        }
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTermAndLang(
                candidat.getIdThesaurus(), candidat.getIdTerm(), candidat.getLang());
        if (!candidat.getEmployePourList().isEmpty()) {
            candidat.getEmployePourList().forEach(employe ->
                    addSynonym(employe, candidat.getIdThesaurus(), candidat.getLang(), candidat.getIdTerm()));
        }
    }

    public void updateCandidateLabel(String label, String thesaurusId, String lang, String termId) {
        persistTermLabel(label, thesaurusId, lang, termId);
    }

    public boolean updateCandidateStatus(String thesaurusId, String conceptId, int status) {
        var candidatStatus = candidatStatusRepository.findAllByIdConceptAndIdThesaurus(conceptId, thesaurusId);
        if (candidatStatus.isEmpty()) {
            return false;
        }
        var newStatus = statusRepository.findById(status);
        if (newStatus.isEmpty()) {
            return false;
        }
        candidatStatus.get().setStatus(newStatus.get());
        candidatStatusRepository.save(candidatStatus.get());
        return true;
    }

    public String migrateOldCandidates(String thesaurusId, int userId) {
        StringBuilder messages = new StringBuilder();
        var nodeCandidateOlds = loadOldModuleCandidates(thesaurusId);
        if (nodeCandidateOlds.isEmpty()) {
            return "Pas d'anciens candidats à récupérer";
        }
        for (NodeCandidateOld nodeCandidateOld : nodeCandidateOlds) {
            nodeCandidateOld.setNodeTraductions(loadOldModuleTranslations(nodeCandidateOld.getIdCandidate(), thesaurusId));
            var propositions = propositionRepository.findAllByIdConceptAndIdThesaurusOrderByCreated(
                    nodeCandidateOld.getIdCandidate(), thesaurusId);
            if (CollectionUtils.isNotEmpty(propositions)) {
                nodeCandidateOld.setNodePropositions(propositions.stream()
                        .map(element -> NodeProposition.builder()
                                .note(element.getNote())
                                .idUser(element.getIdUser())
                                .build())
                        .toList());
            }
        }

        boolean exist = false;
        boolean first = true;
        Concept concept = new Concept();
        String idNewConcept = null;
        String idNewTerm = null;
        Term terme = new Term();

        for (NodeCandidateOld nodeCandidateOld : nodeCandidateOlds) {
            for (NodeTraductionCandidat nodeTraduction : nodeCandidateOld.getNodeTraductions()) {
                if (termRepository.existsPrefLabel(nodeTraduction.getTitle().trim(), nodeTraduction.getIdLang(), thesaurusId)) {
                    messages.append("Candidat existe : ").append(nodeTraduction.getTitle());
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                concept.setIdConcept(null);
                concept.setIdThesaurus(thesaurusId);
                concept.setTopConcept(false);
                concept.setIdUser(userId);
                concept.setStatus("CA");
                try {
                    idNewConcept = createCandidateConcept(concept);
                } catch (SQLException e) {
                    messages.append("Erreur : ").append(nodeCandidateOld.getIdCandidate());
                }
                if (idNewConcept == null) {
                    messages.append("Erreur : ").append(nodeCandidateOld.getIdCandidate());
                    continue;
                }
                for (NodeTraductionCandidat nodeTraduction : nodeCandidateOld.getNodeTraductions()) {
                    if (first) {
                        terme.setIdThesaurus(thesaurusId);
                        terme.setLang(nodeTraduction.getIdLang());
                        terme.setContributor(userId);
                        terme.setLexicalValue(nodeTraduction.getTitle().trim());
                        terme.setSource("candidat");
                        terme.setStatus("D");
                        try {
                            idNewTerm = addTerm(terme, idNewConcept, userId);
                        } catch (SQLException e) {
                            messages.append("Erreur : ").append(nodeCandidateOld.getIdCandidate());
                            continue;
                        }
                        first = false;
                    } else {
                        addTermTranslation(Term.builder()
                                .idTerm(idNewTerm)
                                .idThesaurus(thesaurusId)
                                .lang(nodeTraduction.getIdLang())
                                .lexicalValue(nodeTraduction.getTitle())
                                .source("candidat")
                                .status("D")
                                .build(), userId);
                    }
                }
                first = true;
                for (NodeProposition nodeProposition : nodeCandidateOld.getNodePropositions()) {
                    candidatMessageRepository.save(CandidatMessages.builder()
                            .value(nodeProposition.getNote())
                            .idUser(nodeProposition.getIdUser())
                            .idThesaurus(thesaurusId)
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

    public boolean hasVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) {
        return CollectionUtils.isNotEmpty(candidatVoteRepository.findAllByIdConceptAndIdThesaurusAndIdUserAndIdNoteAndTypeVote(
                conceptId, thesaurusId, userId, noteId, type.getLabel()));
    }

    public void removeVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) {
        candidatVoteRepository.deleteAllByIdUserAndIdConceptAndIdThesaurusAndTypeVoteAndIdNote(
                userId, conceptId, thesaurusId, type.getLabel(), noteId);
    }

    public void addVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) {
        candidatVoteRepository.save(CandidatVote.builder()
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .idUser(userId)
                .idNote(noteId)
                .typeVote(type.getLabel())
                .build());
    }

    public List<NodeIdValue> searchCollections(String thesaurusId, String lang, String query) {
        return conceptGroupLabelRepository.searchGroups(thesaurusId, lang, query).stream()
                .map(element -> NodeIdValue.builder()
                        .id((String) element[0])
                        .value((String) element[1])
                        .build())
                .toList();
    }

    public void addCollection(String groupId, String thesaurusId, String conceptId) {
        conceptGroupConceptRepository.save(ConceptGroupConcept.builder()
                .idGroup(groupId)
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .build());
    }

    public void removeCollection(String groupId, String conceptId, String thesaurusId) {
        conceptGroupConceptRepository.deleteByIdGroupAndIdConceptAndIdThesaurus(groupId, conceptId, thesaurusId);
    }

    public List<NodeIdValue> searchRelationTerms(String query, String lang, String thesaurusId) {
        var processedValue = fr.cnrs.opentheso.utils.StringUtils.unaccentLowerString(
                fr.cnrs.opentheso.utils.StringUtils.convertString(query));
        List<NodeIdValue> results = new ArrayList<>();
        for (Object[] row : searchRepository.searchPreferredLabels(processedValue, lang, thesaurusId)) {
            results.add(NodeIdValue.builder().id((String) row[0]).value((String) row[1]).build());
        }
        for (Object[] row : searchRepository.searchAltLabels(processedValue, lang, thesaurusId)) {
            if (results.stream().noneMatch(element -> ((String) row[1]).equalsIgnoreCase(element.getValue()))) {
                results.add(NodeIdValue.builder().id((String) row[0]).value((String) row[1]).build());
            }
        }
        return results;
    }

    public String resolveUserName(int userId) {
        return userRepository.findById(userId).map(user -> user.getUsername()).orElse("");
    }

    public boolean termExists(String termId, String thesaurusId, String lang) {
        return termRepository.findByIdTermAndIdThesaurusAndLang(termId, thesaurusId, lang).isPresent();
    }

    public void updateTermLabel(String label, String thesaurusId, String lang, String termId) {
        persistTermLabel(label, thesaurusId, lang, termId);
    }

    public void addTerm(Term term) {
        term.setLexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(term.getLexicalValue()));
        termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(term.getIdTerm())
                .lexicalValue(term.getLexicalValue())
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .status(term.getStatus())
                .contributor(term.getContributor())
                .creator(term.getCreator())
                .created(new Date())
                .modified(new Date())
                .build());
    }

    public void addSynonym(String synonym, String thesaurusId, String lang, String termId) {
        nonPreferredTermRepository.save(NonPreferredTerm.builder()
                .lexicalValue(synonym)
                .lang(lang)
                .idThesaurus(thesaurusId)
                .hiden(false)
                .idTerm(termId)
                .created(new Date())
                .modified(new Date())
                .build());
    }

    public void deleteSynonym(String termId, String thesaurusId, String lang, String lexicalValue) {
        nonPreferredTermRepository.deleteByIdThesaurusAndIdTermAndLangAndLexicalValue(
                thesaurusId, termId, lang, lexicalValue);
    }

    public List<NodeNote> loadCandidateNotes(String conceptId, String thesaurusId) {
        return noteRepository.findAllByIdentifierAndIdThesaurus(conceptId, thesaurusId).stream()
                .map(note -> NodeNote.builder()
                        .idNote(note.getId())
                        .noteTypeCode(note.getNoteTypeCode())
                        .idConcept(note.getIdConcept())
                        .lang(note.getLang())
                        .lexicalValue(note.getLexicalValue())
                        .idUser(note.getIdUser())
                        .build())
                .toList();
    }

    public void addBroaderRelation(String conceptId, String thesaurusId, String targetConceptId) {
        addHierarchicalRelation(conceptId, thesaurusId, "BT", targetConceptId);
        addHierarchicalRelation(targetConceptId, thesaurusId, "NT", conceptId);
    }

    public List<NodeIdValue> loadBroaderRelations(String conceptId, String thesaurusId, String lang) {
        return loadRelations(conceptId, thesaurusId, lang, "BT");
    }

    public void deleteBroaderRelation(String conceptId, String thesaurusId, String targetConceptId, int userId) {
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                thesaurusId, conceptId, targetConceptId, "BT");
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                thesaurusId, targetConceptId, conceptId, "NT");
    }

    public void addRelatedTerm(String conceptId, String thesaurusId, String targetConceptId) {
        addHierarchicalRelation(conceptId, thesaurusId, "RT", targetConceptId);
    }

    public List<NodeIdValue> loadRelatedTerms(String conceptId, String thesaurusId, String lang) {
        return loadRelations(conceptId, thesaurusId, lang, "RT");
    }

    public void deleteRelatedTerm(String conceptId, String thesaurusId, String targetConceptId, int userId) {
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                thesaurusId, conceptId, targetConceptId, "RT");
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(
                thesaurusId, targetConceptId, conceptId, "RT");
    }

    public boolean insertCandidate(CandidatDto candidatDto, String adminMessage, int userId) {
        var candidatStatus = candidatStatusRepository.findByIdConcept(candidatDto.getIdConcepte());
        if (candidatStatus.isPresent()) {
            candidatStatus.get().setStatus(statusRepository.findById(2).orElse(null));
            candidatStatus.get().setMessage(adminMessage);
            candidatStatus.get().setIdUserAdmin(userId);
            candidatStatusRepository.save(candidatStatus.get());
            conceptRepository.setStatus("D", candidatDto.getIdConcepte(), candidatDto.getIdThesaurus());
            conceptRepository.setTopConceptTag(CollectionUtils.isEmpty(candidatDto.getTermesGenerique()),
                    candidatDto.getIdConcepte(), candidatDto.getIdThesaurus());
            return false;
        }
        return true;
    }

    public boolean rejectCandidate(CandidatDto candidatDto, String adminMessage, int userId) {
        var candidatStatus = candidatStatusRepository.findByIdConcept(candidatDto.getIdConcepte());
        if (candidatStatus.isPresent()) {
            candidatStatus.get().setStatus(statusRepository.findById(3).orElse(null));
            candidatStatus.get().setMessage(adminMessage);
            candidatStatus.get().setIdUserAdmin(userId);
            candidatStatusRepository.save(candidatStatus.get());
            return false;
        }
        return true;
    }

    public void generateLocalArkForConcepts(String thesaurusId, List<String> conceptIds, Preferences preferences) {
        for (String conceptId : conceptIds) {
            var concept = conceptRepository.findByIdConceptAndIdThesaurus(conceptId, thesaurusId).orElse(null);
            if (concept == null || StringUtils.isNotEmpty(concept.getIdArk())) {
                continue;
            }
            var idArk = ToolsHelper.getNewId(preferences.getSizeIdArkLocal(), preferences.isUppercaseForArk(), true);
            idArk = preferences.getNaanArkLocal() + "/" + preferences.getPrefixArkLocal() + idArk;
            conceptRepository.setIdArk(idArk, new Date(), conceptId, thesaurusId);
        }
    }

    private String createCandidateConcept(Concept concept) throws SQLException {
        concept.setNotation(concept.getNotation() == null ? "" : concept.getNotation());
        concept.setIdArk(concept.getIdArk() == null ? "" : concept.getIdArk());
        var preference = preferencesRepository.findByIdThesaurus(concept.getIdThesaurus()).orElse(null);
        if (concept.getIdConcept() == null) {
            if (preference != null && preference.getIdentifierType() != null && preference.getIdentifierType() == 1) {
                concept.setIdConcept(generateAlphaNumericConceptId());
            } else {
                concept.setIdConcept(generateNumericConceptId());
            }
        }
        if (StringUtils.isEmpty(concept.getIdConcept())) {
            return null;
        }
        conceptRepository.save(fr.cnrs.opentheso.entites.Concept.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .idArk(concept.getIdArk())
                .created(new Date())
                .modified(new Date())
                .status(concept.getStatus())
                .notation(concept.getNotation())
                .topConcept(concept.isTopConcept())
                .creator(concept.getIdUser())
                .conceptType("concept")
                .contributor(concept.getIdUser())
                .gps(false)
                .idDoi("")
                .idHandle("")
                .build());
        conceptHistoriqueRepository.save(fr.cnrs.opentheso.entites.ConceptHistorique.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .idArk(concept.getIdArk())
                .status(concept.getStatus())
                .notation(concept.getNotation())
                .topConcept(concept.isTopConcept())
                .idUser(concept.getIdUser())
                .modified(new Date())
                .build());
        candidatStatusRepository.save(CandidatStatus.builder()
                .idConcept(concept.getIdConcept())
                .idThesaurus(concept.getIdThesaurus())
                .idUser(concept.getIdUser())
                .date(new Date())
                .status(statusRepository.findById(1).orElse(null))
                .build());
        return concept.getIdConcept();
    }

    private String addTerm(Term term, String conceptId, int userId) throws SQLException {
        String idTerm = generateNextTermId(term);
        var termSaved = termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(idTerm)
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(term.getLexicalValue()))
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
                .contributor(userId)
                .creator(userId)
                .created(new Date())
                .modified(new Date())
                .build());
        termHistoriqueRepository.save(TermHistorique.builder()
                .idTerm(termSaved.getIdTerm())
                .lexicalValue(termSaved.getLexicalValue())
                .lang(termSaved.getLang())
                .idThesaurus(termSaved.getIdThesaurus())
                .source(termSaved.getSource())
                .status(termSaved.getStatus())
                .idUser(userId)
                .action("ADD")
                .modified(LocalDateTime.now())
                .build());
        preferredTermRepository.save(PreferredTerm.builder()
                .idConcept(conceptId)
                .idThesaurus(term.getIdThesaurus())
                .idTerm(termSaved.getIdTerm())
                .build());
        return idTerm;
    }

    private void addTermTranslation(Term term, int userId) {
        termRepository.save(fr.cnrs.opentheso.entites.Term.builder()
                .idTerm(term.getIdTerm())
                .lexicalValue(fr.cnrs.opentheso.utils.StringUtils.convertString(term.getLexicalValue()))
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
                .contributor(term.getContributor())
                .creator(term.getCreator())
                .created(new Date())
                .modified(new Date())
                .build());
        termHistoriqueRepository.save(TermHistorique.builder()
                .idTerm(term.getIdTerm())
                .lexicalValue(term.getLexicalValue())
                .lang(term.getLang())
                .idThesaurus(term.getIdThesaurus())
                .source(term.getSource())
                .status(term.getStatus())
                .idUser(userId)
                .modified(LocalDateTime.now())
                .action("New")
                .build());
    }

    private void addNote(String identifier, String idLang, String thesaurusId, String note, String noteTypeCode,
                         String noteSource, int userId) {
        note = fr.cnrs.opentheso.utils.StringUtils.clearValue(note);
        note = fr.cnrs.opentheso.utils.StringUtils.clearNoteFromP(note);
        note = StringEscapeUtils.unescapeXml(note);
        noteRepository.save(Note.builder()
                .noteTypeCode(noteTypeCode)
                .idThesaurus(thesaurusId)
                .lang(idLang)
                .lexicalValue(note)
                .identifier(identifier)
                .noteSource(noteSource)
                .idUser(userId)
                .created(new Date())
                .modified(new Date())
                .build());
    }

    private void persistTermLabel(String label, String thesaurusId, String lang, String termId) {
        termRepository.findByIdTermAndIdThesaurusAndLang(termId, thesaurusId, lang).ifPresent(term -> {
            term.setLexicalValue(label);
            termRepository.save(term);
        });
    }

    private void addHierarchicalRelation(String concept1, String thesaurusId, String role, String concept2) {
        hierarchicalRelationshipRepository.save(HierarchicalRelationship.builder()
                .idConcept1(concept1)
                .idConcept2(concept2)
                .idThesaurus(thesaurusId)
                .role(role)
                .build());
    }

    private void deleteAllRelations(String conceptId, String thesaurusId) {
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept1(thesaurusId, conceptId);
        hierarchicalRelationshipRepository.deleteAllByIdThesaurusAndIdConcept2(thesaurusId, conceptId);
    }

    private List<NodeIdValue> loadRelations(String conceptId, String thesaurusId, String lang, String role) {
        List<HierarchicalRelationship> relations = "BT".equals(role)
                ? hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(thesaurusId, conceptId, "BT")
                : hierarchicalRelationshipRepository.findAllByIdThesaurusAndIdConcept1AndRoleLike(thesaurusId, conceptId, "RT");
        return relations.stream()
                .map(relation -> {
                    String targetId = relation.getIdConcept2();
                    String label = termRepository.getLexicalValueOfConcept(targetId, thesaurusId, lang).orElse("");
                    return NodeIdValue.builder().id(targetId).value(label).build();
                })
                .sorted(Comparator.comparing(NodeIdValue::getValue, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<NodeCandidateOld> loadOldModuleCandidates(String thesaurusId) {
        return conceptCandidatRepository.findAllByIdThesaurusAndStatus(thesaurusId, "a").stream()
                .map(candidat -> NodeCandidateOld.builder()
                        .idCandidate(candidat.getIdConcept())
                        .status(candidat.getStatus())
                        .build())
                .toList();
    }

    private List<NodeTraductionCandidat> loadOldModuleTranslations(String idOldCandidat, String thesaurusId) {
        return conceptTermCandidatRepository.getCandidateTranslations(idOldCandidat, thesaurusId).stream()
                .map(proj -> NodeTraductionCandidat.builder()
                        .idLang(proj.getLang())
                        .title(proj.getLang())
                        .build())
                .toList();
    }

    private String generateAlphaNumericConceptId() {
        String id = ToolsHelper.getNewId(15, false, false);
        while (!conceptRepository.findByIdConcept(id).isEmpty()) {
            id = ToolsHelper.getNewId(15, false, false);
        }
        return id;
    }

    private String generateNumericConceptId() {
        Long idNumerique = conceptRepository.getNextConceptNumericId();
        if (idNumerique == null) {
            throw new IllegalStateException("Impossible de récupérer un ID depuis la séquence concept__id_seq");
        }
        long counter = idNumerique;
        String idConcept = String.valueOf(counter);
        while (!conceptRepository.findByIdConcept(idConcept).isEmpty()) {
            idConcept = String.valueOf(++counter);
        }
        return idConcept;
    }

    private String generateNextTermId(Term term) {
        int idTermNum = termRepository.getMaxInternalId();
        String idTerm;
        do {
            idTerm = String.valueOf(++idTermNum);
        } while (termRepository.findByIdTermAndIdThesaurus(idTerm, term.getIdThesaurus()).isPresent());
        term.setIdTerm(idTerm);
        return idTerm;
    }
}
