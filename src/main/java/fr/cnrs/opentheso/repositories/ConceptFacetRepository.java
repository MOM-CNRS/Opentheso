package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptFacet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface ConceptFacetRepository extends JpaRepository<ConceptFacet, Integer> {

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdFacet(String idThesaurus, String idFacet);

    @Modifying
    @Transactional
    void deleteAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdConceptAndIdThesaurusAndIdFacet(String idConcept, String idThesaurus, String idFacet);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptFacet t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    List<ConceptFacet> findAllByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    List<ConceptFacet> findAllByIdFacetAndIdThesaurus(String idFacet, String idThesaurus);

    List<ConceptFacet> findAllByIdConceptAndIdThesaurusAndIdFacet(String idConcept, String idThesaurus, String idFacet);

    Optional<ConceptFacet> findByIdFacet(String idFacet);

    Optional<ConceptFacet> findByIdFacetAndIdThesaurus(String idFacet, String idThesaurus);

    @Query("""
        SELECT cf.idConcept
        FROM ConceptFacet cf
        WHERE cf.idThesaurus = :idThesaurus AND cf.idFacet = :idFacet
    """)
    List<String> findConceptIdsByFacet(@Param("idThesaurus") String idThesaurus, @Param("idFacet") String idFacet);

    @Query(value = """
        SELECT COUNT(ta.id_facet)
        FROM thesaurus_array ta
        WHERE ta.id_concept_parent = :idConcept
          AND ta.id_thesaurus = :idThesaurus
    """, nativeQuery = true)
    int countFacets(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept);

    @Modifying
    @Transactional
    @Query(value = "UPDATE concept_facet SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    @Procedure(procedureName = "opentheso_add_facet")
    void addFacet(String idFacet, Integer idUser, String idTheso, String idConceptParent, String labels, String members, String notes);

    @Modifying
    @Transactional
    @Query(value = "CALL opentheso_add_facet(:idFacet, :idUser, :idTheso, :idConceptParent, :labels, :membres, :notes)", nativeQuery = true)
    void addFacet(@Param("idFacet") String idFacet, @Param("idUser") int idUser, @Param("idTheso") String idTheso,
                  @Param("idConceptParent") String idConceptParent, @Param("labels") String labels,
                  @Param("membres") String membres, @Param("notes") String notes);
}
