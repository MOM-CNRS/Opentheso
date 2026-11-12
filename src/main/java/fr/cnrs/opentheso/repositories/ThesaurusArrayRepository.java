package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ThesaurusArray;
import fr.cnrs.opentheso.models.NodeFacetProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface ThesaurusArrayRepository extends JpaRepository<ThesaurusArray, Integer> {

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdFacet(String idThesaurus, String idFacet);

    @Modifying
    @Transactional
    @Query("UPDATE ThesaurusArray t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ThesaurusArray t SET t.idConceptParent = :newConceptParent WHERE t.idThesaurus = :idThesaurus AND t.idFacet = :idFacet")
    void updateConceptParent(@Param("newConceptParent") String newConceptParent,
                             @Param("idThesaurus") String idThesaurus,
                             @Param("idFacet") String idFacet);

    @Query("""
        SELECT t.idFacet AS idFacet, t.idConceptParent AS idConceptParent
        FROM ThesaurusArray t
        WHERE t.idFacet = :idFacet AND t.idThesaurus = :idThesaurus
    """)
    Optional<NodeFacetProjection> findFacetMetadata(@Param("idFacet") String idFacet, @Param("idThesaurus") String idThesaurus);

    List<ThesaurusArray> findAllByIdThesaurusAndIdConceptParent(String idThesaurus, String idConceptParent);

    Optional<ThesaurusArray> findAllByIdThesaurusAndIdFacet(String idThesaurus, String idFacet);

    @Query(value = "SELECT last_value FROM public.thesaurus_array_facet_id_seq", nativeQuery = true)
    Long getNextFacetSequenceId();

    @Query(value = """
        SELECT ta.id_concept_parent
        FROM thesaurus_array ta
        WHERE ta.id_facet = :idFacet
        AND ta.id_thesaurus = :idTheso
        AND ta.id_concept_parent IN (
            SELECT cgc.idconcept
            FROM concept_group_concept cgc
            WHERE cgc.idthesaurus = :idTheso
            AND LOWER(cgc.idgroup) IN :groupIds)
        LIMIT 1
    """, nativeQuery = true)
    Optional<String> findConceptParentInGroups(@Param("idFacet") String idFacet, @Param("idTheso") String idTheso, @Param("groupIds") List<String> groupIds);

    @Modifying
    @Query(value = "UPDATE thesaurus_array SET id_thesaurus = :target WHERE id_concept_parent = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurusByParent(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

}
