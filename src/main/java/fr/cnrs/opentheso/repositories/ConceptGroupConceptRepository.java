package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptGroupConcept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface ConceptGroupConceptRepository extends JpaRepository<ConceptGroupConcept, Integer> {

    List<ConceptGroupConcept> findByIdThesaurusAndIdConcept(String idThesaurus, String idConcept);

    List<ConceptGroupConcept> findByIdGroupAndIdThesaurus(String idGroup, String idThesaurus);

    @Modifying
    @Transactional
    void deleteByIdGroupAndIdConceptAndIdThesaurus(String idGroup, String idConcept, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdGroupAndIdThesaurus(String idGroup, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdGroup(String idThesaurus, String idGroup);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdConcept(String idThesaurus, String idConcept);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Query(value = """
        SELECT idgroup, idconcept
        FROM concept_group_concept
        WHERE idthesaurus = :idThesaurus
        AND idconcept NOT IN (SELECT id_concept FROM concept WHERE id_thesaurus = :idThesaurus)
    """, nativeQuery = true)
    List<Object[]> findGroupConceptLinksWithMissingConcepts(@Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptGroupConcept t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Query(value = "UPDATE concept_group_concept SET idthesaurus = :target WHERE idconcept = :concept AND idthesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);
}
