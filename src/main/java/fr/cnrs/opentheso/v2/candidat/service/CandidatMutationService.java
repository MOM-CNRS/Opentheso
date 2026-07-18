package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.entites.NoteType;
import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.MessageDto;
import fr.cnrs.opentheso.models.candidats.TraductionDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.nodes.NodeImage;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.models.users.NodeUser;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.v2.candidat.persistence.CandidatMutationPersistence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatMutationService {

    private final CandidatMutationPersistence candidatMutationPersistence;
    private final ConceptDcTermRepository conceptDcTermRepository;

    public boolean deleteConcept(String conceptId, String thesaurusId) {
        return candidatMutationPersistence.deleteConcept(conceptId, thesaurusId);
    }

    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String lang) {
        return candidatMutationPersistence.loadUsedLanguages(thesaurusId, lang);
    }

    public boolean deleteAlignment(int alignmentId, String thesaurusId) {
        return candidatMutationPersistence.deleteAlignment(alignmentId, thesaurusId);
    }

    public List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId) {
        return candidatMutationPersistence.loadAlignments(conceptId, thesaurusId);
    }

    public void updateAlignment(AlignementElement element, String conceptId, String thesaurusId) {
        candidatMutationPersistence.updateAlignment(element, conceptId, thesaurusId);
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
        return candidatMutationPersistence.saveNewCandidat(candidat, thesaurusId, lang, userId, username, thesaurusLang, definition);
    }

    public void updateCandidateDetails(CandidatDto candidat) {
        candidatMutationPersistence.updateCandidateDetails(candidat);
    }

    public void updateCandidateLabel(String label, String thesaurusId, String lang, String termId) {
        candidatMutationPersistence.updateCandidateLabel(label, thesaurusId, lang, termId);
    }

    public boolean updateCandidateStatus(String thesaurusId, String conceptId, int status) {
        return candidatMutationPersistence.updateCandidateStatus(thesaurusId, conceptId, status);
    }

    public String migrateOldCandidates(String thesaurusId, int userId) {
        return candidatMutationPersistence.migrateOldCandidates(thesaurusId, userId);
    }

    public boolean hasVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        return candidatMutationPersistence.hasVote(thesaurusId, conceptId, userId, noteId, type);
    }

    public void removeVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        candidatMutationPersistence.removeVote(thesaurusId, conceptId, userId, noteId, type);
    }

    public void addVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        candidatMutationPersistence.addVote(thesaurusId, conceptId, userId, noteId, type);
    }

    public List<NodeIdValue> searchCollections(String thesaurusId, String lang, String query) {
        return candidatMutationPersistence.searchCollections(thesaurusId, lang, query);
    }

    public void addCollection(String groupId, String thesaurusId, String conceptId) {
        candidatMutationPersistence.addCollection(groupId, thesaurusId, conceptId);
    }

    public void removeCollection(String groupId, String conceptId, String thesaurusId) {
        candidatMutationPersistence.removeCollection(groupId, conceptId, thesaurusId);
    }

    public List<NodeIdValue> searchRelationTerms(String query, String lang, String thesaurusId) {
        return candidatMutationPersistence.searchRelationTerms(query, lang, thesaurusId);
    }

    public String resolveUserName(int userId) {
        return candidatMutationPersistence.resolveUserName(userId);
    }

    public boolean termExists(String termId, String thesaurusId, String lang) {
        return candidatMutationPersistence.termExists(termId, thesaurusId, lang);
    }

    public void updateTermLabel(String label, String thesaurusId, String lang, String termId) {
        candidatMutationPersistence.updateTermLabel(label, thesaurusId, lang, termId);
    }

    public void addTerm(Term term) {
        candidatMutationPersistence.addTerm(term);
    }

    public void addSynonym(String synonym, String thesaurusId, String lang, String termId) {
        candidatMutationPersistence.addSynonym(synonym, thesaurusId, lang, termId);
    }

    public void deleteSynonym(String termId, String thesaurusId, String lang, String lexicalValue) {
        candidatMutationPersistence.deleteSynonym(termId, thesaurusId, lang, lexicalValue);
    }

    public List<NodeNote> loadCandidateNotes(String conceptId, String thesaurusId) {
        return candidatMutationPersistence.loadCandidateNotes(conceptId, thesaurusId);
    }

    public void saveContributorMetadata(String conceptId, String thesaurusId, String contributorName) {
        conceptDcTermRepository.save(ConceptDcTerm.builder()
                .name(DCMIResource.CREATOR)
                .value(contributorName)
                .idConcept(conceptId)
                .idThesaurus(thesaurusId)
                .build());
    }

    public void addBroaderRelation(String conceptId, String thesaurusId, String targetConceptId) throws SQLException {
        candidatMutationPersistence.addBroaderRelation(conceptId, thesaurusId, targetConceptId);
    }

    public List<NodeIdValue> loadBroaderRelations(String conceptId, String thesaurusId, String lang) {
        return candidatMutationPersistence.loadBroaderRelations(conceptId, thesaurusId, lang);
    }

    public void deleteBroaderRelation(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException {
        candidatMutationPersistence.deleteBroaderRelation(conceptId, thesaurusId, targetConceptId, userId);
    }

    public void addRelatedTerm(String conceptId, String thesaurusId, String targetConceptId) throws SQLException {
        candidatMutationPersistence.addRelatedTerm(conceptId, thesaurusId, targetConceptId);
    }

    public List<NodeIdValue> loadRelatedTerms(String conceptId, String thesaurusId, String lang) {
        return candidatMutationPersistence.loadRelatedTerms(conceptId, thesaurusId, lang);
    }

    public void deleteRelatedTerm(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException {
        candidatMutationPersistence.deleteRelatedTerm(conceptId, thesaurusId, targetConceptId, userId);
    }

    public List<NoteType> loadNoteTypes() {
        return candidatMutationPersistence.loadNoteTypes();
    }

    public void addOrUpdateCandidateNote(String identifier, String idLang, String idThesaurus, String note,
                                          String noteTypeCode, String noteSource, int idUser) {
        candidatMutationPersistence.addOrUpdateCandidateNote(identifier, idLang, idThesaurus, note, noteTypeCode, noteSource, idUser);
    }

    public boolean updateCandidateNote(int idNote, String idConcept, String idLang, String idThesaurus,
                                        String note, String noteSource, String noteTypeCode, int idUser) {
        return candidatMutationPersistence.updateCandidateNote(idNote, idConcept, idLang, idThesaurus, note, noteSource, noteTypeCode, idUser);
    }

    public void deleteCandidateNote(int idNote, String identifier, String idLang, String idThesaurus,
                                     String noteTypeCode, String oldNote, int idUser) {
        candidatMutationPersistence.deleteCandidateNote(idNote, identifier, idLang, idThesaurus, noteTypeCode, oldNote, idUser);
    }

    public boolean isLabelExistIgnoreCase(String label, String thesaurusId, String lang) {
        return candidatMutationPersistence.isLabelExistIgnoreCase(label, thesaurusId, lang);
    }

    public void deleteCandidateTranslation(String thesaurusId, String termId, String lang) {
        candidatMutationPersistence.deleteCandidateTranslation(thesaurusId, termId, lang);
    }

    public void addCandidateTranslation(Term term, int userId) {
        candidatMutationPersistence.addCandidateTranslation(term, userId);
    }

    public List<TraductionDto> loadCandidateTranslations(String conceptId, String thesaurusId, String lang) {
        return candidatMutationPersistence.loadCandidateTranslations(conceptId, thesaurusId, lang);
    }

    public List<NodeImage> loadExternalImages(String thesaurusId, String conceptId) {
        return candidatMutationPersistence.loadExternalImages(thesaurusId, conceptId);
    }

    public void addExternalImage(String conceptId, String thesaurusId, String imageName, String copyright,
                                  String uri, String creator, int userId) {
        candidatMutationPersistence.addExternalImage(conceptId, thesaurusId, imageName, copyright, uri, creator, userId);
    }

    public void deleteExternalImage(String thesaurusId, String conceptId, String uri) {
        candidatMutationPersistence.deleteExternalImage(thesaurusId, conceptId, uri);
    }

    public void sendDiscussionMessage(String conceptId, String thesaurusId, String message, int userId) {
        candidatMutationPersistence.sendDiscussionMessage(conceptId, thesaurusId, message, userId);
    }

    public List<NodeUser> loadDiscussionParticipants(String conceptId, String thesaurusId) {
        return candidatMutationPersistence.loadDiscussionParticipants(conceptId, thesaurusId);
    }

    public List<MessageDto> loadDiscussionMessages(String conceptId, String thesaurusId, int currentUserId) {
        return candidatMutationPersistence.loadDiscussionMessages(conceptId, thesaurusId, currentUserId);
    }

    public void notifyDiscussionParticipants(String conceptId, String thesaurusId, String conceptLabel) {
        candidatMutationPersistence.notifyDiscussionParticipants(conceptId, thesaurusId, conceptLabel);
    }

    public void sendMailInvitation(String email) {
        candidatMutationPersistence.sendMailInvitation(email);
    }
}
