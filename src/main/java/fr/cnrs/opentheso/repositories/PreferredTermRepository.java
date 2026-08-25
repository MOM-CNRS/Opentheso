package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.PreferredTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface PreferredTermRepository extends JpaRepository<PreferredTerm, Integer> {

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdTerm(String idThesaurus, String idTerm);

    @Modifying
    @Transactional
    @Query("DELETE FROM PreferredTerm p WHERE p.idThesaurus = :idThesaurus")
    void deleteByIdThesaurus(@Param("idThesaurus") String idThesaurus);

    Optional<PreferredTerm> findByIdThesaurusAndIdConcept(String idThesaurus, String idConcept);

    List<PreferredTerm> findByIdThesaurusAndIdConceptIn(String idThesaurus, java.util.Set<String> idConcepts);

    @Modifying
    @Transactional
    @Query("UPDATE PreferredTerm t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
        SELECT DISTINCT pt.id_concept
        FROM preferred_term pt
        JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
        WHERE t.id_thesaurus = :idThesaurus
          AND LOWER(unaccent(t.lexical_value)) LIKE LOWER(CONCAT('%', unaccent(:label), '%'))
          AND t.lang = :lang
    """, nativeQuery = true)
    Optional<String> findConceptIdByLabel(@Param("label") String label, @Param("idThesaurus") String idThesaurus, @Param("lang") String lang);

    @Modifying
    @Query(value = "UPDATE preferred_term SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

}
