package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.CandidatMessages;
import fr.cnrs.opentheso.models.CandidatMessageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface CandidatMessageRepository extends JpaRepository<CandidatMessages, Integer> {

    @Query(value = """
        SELECT u.id_user AS idUser, u.username AS username, c.value AS value, c.date AS date
        FROM candidat_messages c JOIN users u ON c.id_user = u.id_user
        WHERE c.id_concept = :idConcept
        AND c.id_thesaurus = :idThesaurus
        ORDER BY c.date
    """, nativeQuery = true)
    List<CandidatMessageProjection> findMessagesByConceptAndThesaurus(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE CandidatMessages t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Query(value = "UPDATE candidat_messages SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

}
