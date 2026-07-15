package fr.cnrs.opentheso.v2.candidat.service;

import fr.cnrs.opentheso.entites.ConceptDcTerm;
import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.concept.DCMIResource;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;
import fr.cnrs.opentheso.repositories.ConceptDcTermRepository;
import fr.cnrs.opentheso.v2.candidat.session.CandidatMutationLegacySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatMutationService {

    private final CandidatMutationLegacySupport legacySupport;
    private final ConceptDcTermRepository conceptDcTermRepository;

    public boolean deleteConcept(String conceptId, String thesaurusId) {
        return legacySupport.deleteConcept(conceptId, thesaurusId);
    }

    public List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String lang) {
        return legacySupport.loadUsedLanguages(thesaurusId, lang);
    }

    public boolean deleteAlignment(int alignmentId, String thesaurusId) {
        return legacySupport.deleteAlignment(alignmentId, thesaurusId);
    }

    public List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId) {
        return legacySupport.loadAlignments(conceptId, thesaurusId);
    }

    public void updateAlignment(AlignementElement element, String conceptId, String thesaurusId) {
        legacySupport.updateAlignment(element, conceptId, thesaurusId);
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
        return legacySupport.saveNewCandidat(candidat, thesaurusId, lang, userId, username, thesaurusLang, definition);
    }

    public void updateCandidateDetails(CandidatDto candidat) {
        legacySupport.updateCandidateDetails(candidat);
    }

    public void updateCandidateLabel(String label, String thesaurusId, String lang, String termId) {
        legacySupport.updateCandidateLabel(label, thesaurusId, lang, termId);
    }

    public boolean updateCandidateStatus(String thesaurusId, String conceptId, int status) {
        return legacySupport.updateCandidateStatus(thesaurusId, conceptId, status);
    }

    public String migrateOldCandidates(String thesaurusId, int userId) {
        return legacySupport.migrateOldCandidates(thesaurusId, userId);
    }

    public boolean hasVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        return legacySupport.hasVote(thesaurusId, conceptId, userId, noteId, type);
    }

    public void removeVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        legacySupport.removeVote(thesaurusId, conceptId, userId, noteId, type);
    }

    public void addVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException {
        legacySupport.addVote(thesaurusId, conceptId, userId, noteId, type);
    }

    public List<NodeIdValue> searchCollections(String thesaurusId, String lang, String query) {
        return legacySupport.searchCollections(thesaurusId, lang, query);
    }

    public void addCollection(String groupId, String thesaurusId, String conceptId) {
        legacySupport.addCollection(groupId, thesaurusId, conceptId);
    }

    public void removeCollection(String groupId, String conceptId, String thesaurusId) {
        legacySupport.removeCollection(groupId, conceptId, thesaurusId);
    }

    public List<NodeIdValue> searchRelationTerms(String query, String lang, String thesaurusId) {
        return legacySupport.searchRelationTerms(query, lang, thesaurusId);
    }

    public String resolveUserName(int userId) {
        return legacySupport.resolveUserName(userId);
    }

    public boolean termExists(String termId, String thesaurusId, String lang) {
        return legacySupport.termExists(termId, thesaurusId, lang);
    }

    public void updateTermLabel(String label, String thesaurusId, String lang, String termId) {
        legacySupport.updateTermLabel(label, thesaurusId, lang, termId);
    }

    public void addTerm(Term term) {
        legacySupport.addTerm(term);
    }

    public void addSynonym(String synonym, String thesaurusId, String lang, String termId) {
        legacySupport.addSynonym(synonym, thesaurusId, lang, termId);
    }

    public void deleteSynonym(String termId, String thesaurusId, String lang, String lexicalValue) {
        legacySupport.deleteSynonym(termId, thesaurusId, lang, lexicalValue);
    }

    public List<NodeNote> loadCandidateNotes(String conceptId, String thesaurusId) {
        return legacySupport.loadCandidateNotes(conceptId, thesaurusId);
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
        legacySupport.addBroaderRelation(conceptId, thesaurusId, targetConceptId);
    }

    public List<NodeIdValue> loadBroaderRelations(String conceptId, String thesaurusId, String lang) {
        return legacySupport.loadBroaderRelations(conceptId, thesaurusId, lang);
    }

    public void deleteBroaderRelation(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException {
        legacySupport.deleteBroaderRelation(conceptId, thesaurusId, targetConceptId, userId);
    }

    public void addRelatedTerm(String conceptId, String thesaurusId, String targetConceptId) throws SQLException {
        legacySupport.addRelatedTerm(conceptId, thesaurusId, targetConceptId);
    }

    public List<NodeIdValue> loadRelatedTerms(String conceptId, String thesaurusId, String lang) {
        return legacySupport.loadRelatedTerms(conceptId, thesaurusId, lang);
    }

    public void deleteRelatedTerm(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException {
        legacySupport.deleteRelatedTerm(conceptId, thesaurusId, targetConceptId, userId);
    }
}
