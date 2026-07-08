package fr.cnrs.opentheso.v2.candidat.persistence;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.v2.candidat.session.CandidatMutationLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Primary
@Component
@RequiredArgsConstructor
public class V2NativeCandidatMutationSupport implements CandidatMutationLegacySupport {

    private final CandidatMutationPersistence candidatMutationPersistence;

    @Override
    public boolean deleteConcept(String conceptId, String thesaurusId) {
        return candidatMutationPersistence.deleteConcept(conceptId, thesaurusId);
    }

    @Override
    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String lang) {
        return candidatMutationPersistence.loadUsedLanguages(thesaurusId, lang);
    }

    @Override
    public boolean deleteAlignment(int alignmentId, String thesaurusId) {
        return candidatMutationPersistence.deleteAlignment(alignmentId, thesaurusId);
    }

    @Override
    public List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId) {
        return candidatMutationPersistence.loadAlignments(conceptId, thesaurusId);
    }

    @Override
    public void updateAlignment(AlignementElement element, String conceptId, String thesaurusId) {
        candidatMutationPersistence.updateAlignment(element, conceptId, thesaurusId);
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
        return candidatMutationPersistence.saveNewCandidat(
                candidat, thesaurusId, lang, userId, username, thesaurusLang, definition);
    }

    @Override
    public void updateCandidateDetails(CandidatDto candidat) {
        candidatMutationPersistence.updateCandidateDetails(candidat);
    }

    @Override
    public void updateCandidateLabel(String label, String thesaurusId, String lang, String termId) {
        candidatMutationPersistence.updateCandidateLabel(label, thesaurusId, lang, termId);
    }

    @Override
    public boolean updateCandidateStatus(String thesaurusId, String conceptId, int status) {
        return candidatMutationPersistence.updateCandidateStatus(thesaurusId, conceptId, status);
    }

    @Override
    public String migrateOldCandidates(String thesaurusId, int userId) {
        return candidatMutationPersistence.migrateOldCandidates(thesaurusId, userId);
    }

    @Override
    public boolean hasVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type)
            throws SQLException {
        return candidatMutationPersistence.hasVote(thesaurusId, conceptId, userId, noteId, type);
    }

    @Override
    public void removeVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type)
            throws SQLException {
        candidatMutationPersistence.removeVote(thesaurusId, conceptId, userId, noteId, type);
    }

    @Override
    public void addVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type)
            throws SQLException {
        candidatMutationPersistence.addVote(thesaurusId, conceptId, userId, noteId, type);
    }

    @Override
    public List<NodeIdValue> searchCollections(String thesaurusId, String lang, String query) {
        return candidatMutationPersistence.searchCollections(thesaurusId, lang, query);
    }

    @Override
    public void addCollection(String groupId, String thesaurusId, String conceptId) {
        candidatMutationPersistence.addCollection(groupId, thesaurusId, conceptId);
    }

    @Override
    public void removeCollection(String groupId, String conceptId, String thesaurusId) {
        candidatMutationPersistence.removeCollection(groupId, conceptId, thesaurusId);
    }

    @Override
    public List<NodeIdValue> searchRelationTerms(String query, String lang, String thesaurusId) {
        return candidatMutationPersistence.searchRelationTerms(query, lang, thesaurusId);
    }

    @Override
    public String resolveUserName(int userId) {
        return candidatMutationPersistence.resolveUserName(userId);
    }

    @Override
    public boolean termExists(String termId, String thesaurusId, String lang) {
        return candidatMutationPersistence.termExists(termId, thesaurusId, lang);
    }

    @Override
    public void updateTermLabel(String label, String thesaurusId, String lang, String termId) {
        candidatMutationPersistence.updateTermLabel(label, thesaurusId, lang, termId);
    }

    @Override
    public void addTerm(Term term) {
        candidatMutationPersistence.addTerm(term);
    }

    @Override
    public void addSynonym(String synonym, String thesaurusId, String lang, String termId) {
        candidatMutationPersistence.addSynonym(synonym, thesaurusId, lang, termId);
    }

    @Override
    public void deleteSynonym(String termId, String thesaurusId, String lang, String lexicalValue) {
        candidatMutationPersistence.deleteSynonym(termId, thesaurusId, lang, lexicalValue);
    }

    @Override
    public List<NodeNote> loadCandidateNotes(String conceptId, String thesaurusId) {
        return candidatMutationPersistence.loadCandidateNotes(conceptId, thesaurusId);
    }

    @Override
    public void addBroaderRelation(String conceptId, String thesaurusId, String targetConceptId) throws SQLException {
        candidatMutationPersistence.addBroaderRelation(conceptId, thesaurusId, targetConceptId);
    }

    @Override
    public List<NodeIdValue> loadBroaderRelations(String conceptId, String thesaurusId, String lang) {
        return candidatMutationPersistence.loadBroaderRelations(conceptId, thesaurusId, lang);
    }

    @Override
    public void deleteBroaderRelation(String conceptId, String thesaurusId, String targetConceptId, int userId)
            throws SQLException {
        candidatMutationPersistence.deleteBroaderRelation(conceptId, thesaurusId, targetConceptId, userId);
    }

    @Override
    public void addRelatedTerm(String conceptId, String thesaurusId, String targetConceptId) throws SQLException {
        candidatMutationPersistence.addRelatedTerm(conceptId, thesaurusId, targetConceptId);
    }

    @Override
    public List<NodeIdValue> loadRelatedTerms(String conceptId, String thesaurusId, String lang) {
        return candidatMutationPersistence.loadRelatedTerms(conceptId, thesaurusId, lang);
    }

    @Override
    public void deleteRelatedTerm(String conceptId, String thesaurusId, String targetConceptId, int userId)
            throws SQLException {
        candidatMutationPersistence.deleteRelatedTerm(conceptId, thesaurusId, targetConceptId, userId);
    }
}
