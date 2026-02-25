package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.CandidatStatus;
import fr.cnrs.opentheso.models.CandidateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface CandidatStatusRepository extends JpaRepository<CandidatStatus, Integer> {

    Optional<CandidatStatus> findByIdConcept(String idConcept);

    Optional<CandidatStatus> findAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE CandidatStatus t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
        SELECT cs.id_concept as idConcept, c.created as created, c.modified as modified, cs.id_user as idUser, cs.id_user_admin as idUserAdmin,  cs.message as message
        FROM candidat_status cs JOIN concept c ON cs.id_concept = c.id_concept AND cs.id_thesaurus = c.id_thesaurus
        WHERE cs.id_status = :status
        AND cs.id_thesaurus = :thesaurusId
        ORDER BY c.created DESC
    """, nativeQuery = true)
    List<CandidateProjection> findCandidatesByStatus(@Param("thesaurusId") String thesaurusId, @Param("status") int status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE candidat_status SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);
}
