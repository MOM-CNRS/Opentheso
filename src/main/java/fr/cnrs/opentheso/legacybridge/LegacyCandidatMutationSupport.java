package fr.cnrs.opentheso.legacybridge;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.services.AlignmentService;
import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.ConceptService;
import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.NonPreferredTermService;
import fr.cnrs.opentheso.services.NoteService;
import fr.cnrs.opentheso.services.RelationService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.services.TermService;
import fr.cnrs.opentheso.services.ThesaurusService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.v2.candidat.session.CandidatMutationLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LegacyCandidatMutationSupport implements CandidatMutationLegacySupport {

    private final ConceptService conceptService;
    private final AlignmentService alignmentService;
    private final CandidatService candidatService;
    private final TermService termService;
    private final GroupService groupService;
    private final SearchService searchService;
    private final UserService userService;
    private final NoteService noteService;
    private final RelationService relationService;
    private final NonPreferredTermService nonPreferredTermService;
    private final ThesaurusService thesaurusService;

    @Override
    public boolean deleteConcept(String conceptId, String thesaurusId) {
        return conceptService.deleteConcept(conceptId, thesaurusId);
    }

    @Override
    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String lang) {
        return thesaurusService.getAllUsedLanguagesOfThesaurusNode(thesaurusId, lang);
    }

    @Override
    public boolean deleteAlignment(int alignmentId, String thesaurusId) {
        return alignmentService.deleteAlignment(alignmentId, thesaurusId);
    }

    @Override
    public List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId) {
        return alignmentService.getAllAlignmentOfConcept(conceptId, thesaurusId);
    }

    @Override
    public void updateAlignment(AlignementElement element, String conceptId, String thesaurusId) {
        alignmentService.updateAlignement(element, conceptId, thesaurusId);
    }

    @Override
    public boolean saveNewCandidat(
            CandidatDto candidat,
            String thesaurusId,
            String lang,
            int userId,
            String username,
            String thesaurusLang,
            String definition
    ) throws SQLException, IOException {
        return candidatService.saveNewCandidat(candidat, thesaurusId, lang, userId, username, thesaurusLang, definition);
    }

    @Override
    public void updateCandidateDetails(CandidatDto candidat) {
        candidatService.updateDetailsCondidat(candidat);
    }

    @Override
    public void updateCandidateLabel(String label, String thesaurusId, String lang, String termId) {
        candidatService.updateIntitule(label, thesaurusId, lang, termId);
    }

    @Override
    public boolean updateCandidateStatus(String thesaurusId, String conceptId, int status) {
        return candidatService.updateCandidatStatus(thesaurusId, conceptId, status);
    }

    @Override
    public String migrateOldCandidates(String thesaurusId, int userId) {
        return candidatService.getOldCandidates(thesaurusId, userId);
    }

    @Override
    public boolean hasVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        return candidatService.isHaveVote(thesaurusId, conceptId, userId, noteId, type);
    }

    @Override
    public void removeVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        candidatService.removeVote(thesaurusId, conceptId, userId, noteId, type);
    }

    @Override
    public void addVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        candidatService.addVote(thesaurusId, conceptId, userId, noteId, type);
    }

    @Override
    public List<NodeIdValue> searchCollections(String thesaurusId, String lang, String query) {
        return groupService.searchGroup(thesaurusId, lang, query);
    }

    @Override
    public void addCollection(String groupId, String thesaurusId, String conceptId) {
        groupService.addNewDomaine(groupId, thesaurusId, conceptId);
    }

    @Override
    public void removeCollection(String groupId, String conceptId, String thesaurusId) {
        groupService.deleteRelationConceptGroupConcept(groupId, conceptId, thesaurusId);
    }

    @Override
    public List<NodeIdValue> searchRelationTerms(String query, String lang, String thesaurusId) {
        return searchService.searchAutoCompletionForRelationIdValue(query, lang, thesaurusId);
    }

    @Override
    public String resolveUserName(int userId) {
        var user = userService.getUser(userId);
        return user != null ? user.getName() : "";
    }

    @Override
    public boolean termExists(String termId, String thesaurusId, String lang) {
        return termService.isTermExistInLangAndThesaurus(termId, thesaurusId, lang);
    }

    @Override
    public void updateTermLabel(String label, String thesaurusId, String lang, String termId) {
        termService.updateIntitule(label, thesaurusId, lang, termId);
    }

    @Override
    public void addTerm(Term term) {
        termService.addNewTerme(term);
    }

    @Override
    public void addSynonym(String synonym, String thesaurusId, String lang, String termId) {
        termService.addSynonyme(synonym, thesaurusId, lang, termId);
    }

    @Override
    public void deleteSynonym(String termId, String thesaurusId, String lang, String lexicalValue) {
        nonPreferredTermService.deleteEMByIdTermAndLangAndLexical(termId, thesaurusId, lang, lexicalValue);
    }

    @Override
    public List<NodeNote> loadCandidateNotes(String conceptId, String thesaurusId) {
        return noteService.getNotesCandidat(conceptId, thesaurusId);
    }

    @Override
    public void addBroaderRelation(String conceptId, String thesaurusId, String targetConceptId) throws SQLException {
        relationService.addHierarchicalRelation(conceptId, thesaurusId, "BT", targetConceptId);
        relationService.addHierarchicalRelation(targetConceptId, thesaurusId, "NT", conceptId);
    }

    @Override
    public List<NodeIdValue> loadBroaderRelations(String conceptId, String thesaurusId, String lang) {
        return relationService.getCandidatRelationsBT(conceptId, thesaurusId, lang);
    }

    @Override
    public void deleteBroaderRelation(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException {
        relationService.deleteRelationBT(conceptId, thesaurusId, targetConceptId, userId);
    }

    @Override
    public void addRelatedTerm(String conceptId, String thesaurusId, String targetConceptId) throws SQLException {
        relationService.addHierarchicalRelation(conceptId, thesaurusId, "RT", targetConceptId);
    }

    @Override
    public List<NodeIdValue> loadRelatedTerms(String conceptId, String thesaurusId, String lang) {
        return relationService.getCandidatRelationsRT(conceptId, thesaurusId, lang);
    }

    @Override
    public void deleteRelatedTerm(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException {
        relationService.deleteRelationRT(conceptId, thesaurusId, targetConceptId, userId);
    }
}
