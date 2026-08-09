package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.CandidatVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;


public interface CandidatVoteRepository extends JpaRepository<CandidatVote, Integer> {

    List<CandidatVote> findAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    List<CandidatVote> findAllByIdConceptAndIdThesaurusAndTypeVote(String idConcept, String idThesaurus, String typeVote);

    List<CandidatVote> findAllByIdConceptAndIdThesaurusAndIdUserAndTypeVote(String idConcept, String idThesaurus,
                                                                            Integer idUser, String typeVote);

    List<CandidatVote> findAllByIdConceptAndIdThesaurusAndIdUserAndIdNoteAndTypeVote(String idConcept, String idThesaurus,
                                                                                     Integer idUser, String idNote, String typeVote);
    @Modifying
    @Transactional
    void deleteAllByIdUserAndIdConceptAndIdThesaurusAndTypeVoteAndIdNote(Integer user, String idConcept, String idThesaurus,
                                                                         String typeVote, String idNote);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdConceptAndIdNote(String idThesaurus, String idConcept, String idNote);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM candidat_vote WHERE id_concept = :conceptId AND id_thesaurus = :thesaurusId", nativeQuery = true)
    int deleteAllByIdThesaurusAndIdConcept(@Param("thesaurusId") String thesaurusId, @Param("conceptId") String conceptId);

    @Modifying
    @Transactional
    @Query("UPDATE CandidatVote t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Query(value = "UPDATE candidat_vote SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);
}
