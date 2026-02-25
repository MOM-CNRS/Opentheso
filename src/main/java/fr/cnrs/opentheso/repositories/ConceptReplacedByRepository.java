package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptReplacedBy;
import fr.cnrs.opentheso.models.ReplacedByProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ConceptReplacedByRepository extends JpaRepository<ConceptReplacedBy, Integer> {

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptReplacedBy t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdConcept1AndIdThesaurus(String idConcept1, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdConcept2AndIdThesaurus(String idConcept2, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdConcept1AndIdConcept2AndIdThesaurus(String idConcept1, String idConcept2, String idThesaurus);

    @Modifying
    @Transactional
    @Query(value = "UPDATE concept_replacedby SET id_thesaurus = :target WHERE id_concept1 = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurusByConcept1(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    @Modifying
    @Transactional
    @Query(value = "UPDATE concept_replacedby SET id_thesaurus = :target WHERE id_concept2 = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurusByConcept2(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    List<ConceptReplacedBy> findAllByIdConcept1AndIdThesaurus(String idConcept1, String idThesaurus);

    List<ConceptReplacedBy> findAllByIdConcept2AndIdThesaurus(String idConcept2, String idThesaurus);

    @Query(value = """
        SELECT c.id_concept AS idConcept, c.id_ark AS idArk, c.id_handle AS idHandle, c.id_doi AS idDoi
        FROM concept_replacedby cr
            JOIN concept c ON c.id_concept = cr.id_concept2 AND c.id_thesaurus = cr.id_thesaurus
        WHERE cr.id_concept1 = :idConcept AND cr.id_thesaurus = :idThesaurus
    """, nativeQuery = true)
    List<ReplacedByProjection> findReplacedByWithUri(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept);

    @Query(value = """
        SELECT c.id_concept AS idConcept,
               c.id_ark AS idArk,
               c.id_handle AS idHandle,
               c.id_doi AS idDoi
        FROM concept_replacedby cr
        JOIN concept c ON c.id_concept = cr.id_concept1 AND c.id_thesaurus = cr.id_thesaurus
        WHERE cr.id_concept2 = :idConcept AND cr.id_thesaurus = :idThesaurus
    """, nativeQuery = true)
    List<ReplacedByProjection> findReplacesWithUri(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept);
}
