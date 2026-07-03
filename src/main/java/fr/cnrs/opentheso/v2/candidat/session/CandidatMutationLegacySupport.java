package fr.cnrs.opentheso.v2.candidat.session;

import fr.cnrs.opentheso.models.alignment.AlignementElement;
import fr.cnrs.opentheso.models.alignment.NodeAlignment;
import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.notes.NodeNote;
import fr.cnrs.opentheso.models.terms.Term;
import fr.cnrs.opentheso.models.thesaurus.NodeLangTheso;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface CandidatMutationLegacySupport {

    boolean deleteConcept(String conceptId, String thesaurusId);

    List<NodeLangTheso> loadUsedLanguages(String thesaurusId, String lang);

    boolean deleteAlignment(int alignmentId, String thesaurusId);

    List<NodeAlignment> loadAlignments(String conceptId, String thesaurusId);

    void updateAlignment(AlignementElement element, String conceptId, String thesaurusId);

    boolean saveNewCandidat(
            CandidatDto candidat,
            String thesaurusId,
            String lang,
            int userId,
            String username,
            String thesaurusLang,
            String definition
    ) throws SQLException, IOException;

    void updateCandidateDetails(CandidatDto candidat);

    void updateCandidateLabel(String label, String thesaurusId, String lang, String termId);

    boolean updateCandidateStatus(String thesaurusId, String conceptId, int status);

    String migrateOldCandidates(String thesaurusId, int userId);

    boolean hasVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException;

    void removeVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException;

    void addVote(String thesaurusId, String conceptId, int userId, String noteId, VoteType type) throws SQLException;

    List<NodeIdValue> searchCollections(String thesaurusId, String lang, String query);

    void addCollection(String groupId, String thesaurusId, String conceptId);

    void removeCollection(String groupId, String conceptId, String thesaurusId);

    List<NodeIdValue> searchRelationTerms(String query, String lang, String thesaurusId);

    String resolveUserName(int userId);

    boolean termExists(String termId, String thesaurusId, String lang);

    void updateTermLabel(String label, String thesaurusId, String lang, String termId);

    void addTerm(Term term);

    void addSynonym(String synonym, String thesaurusId, String lang, String termId);

    void deleteSynonym(String termId, String thesaurusId, String lang, String lexicalValue);

    List<NodeNote> loadCandidateNotes(String conceptId, String thesaurusId);

    void addBroaderRelation(String conceptId, String thesaurusId, String targetConceptId) throws SQLException;

    List<NodeIdValue> loadBroaderRelations(String conceptId, String thesaurusId, String lang);

    void deleteBroaderRelation(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException;

    void addRelatedTerm(String conceptId, String thesaurusId, String targetConceptId) throws SQLException;

    List<NodeIdValue> loadRelatedTerms(String conceptId, String thesaurusId, String lang);

    void deleteRelatedTerm(String conceptId, String thesaurusId, String targetConceptId, int userId) throws SQLException;
}
