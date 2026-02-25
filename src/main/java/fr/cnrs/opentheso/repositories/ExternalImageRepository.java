package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ImageExterne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


public interface ExternalImageRepository extends JpaRepository<ImageExterne, Integer> {

    @Modifying
    @Transactional
    @Query(value = "UPDATE external_images SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    @Modifying
    @Transactional
    @Query(value = "UPDATE external_images SET id_thesaurus = :newIdThesaurus WHERE id_thesaurus = :oldIdThesaurus", nativeQuery = true)
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);


    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);
}
