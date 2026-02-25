package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.HierarchicalRelationship;
import fr.cnrs.opentheso.models.BroaderRelationProjection;
import fr.cnrs.opentheso.models.CustomRelationProjection;
import fr.cnrs.opentheso.models.NodeHieraRelationProjection;
import fr.cnrs.opentheso.models.NodeNTProjection;
import fr.cnrs.opentheso.models.NodeTreeView;
import fr.cnrs.opentheso.models.RelatedRelationProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface HierarchicalRelationshipRepository extends JpaRepository<HierarchicalRelationship, Integer> {

    List<HierarchicalRelationship> findAllByIdThesaurusAndIdConcept1AndRoleLike(String idThesaurus, String idConcept1, String roleLike);

    List<HierarchicalRelationship> findAllByIdThesaurusAndIdConcept2AndRoleLike(String idThesaurus, String idConcept2, String roleLike);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String idThesaurus);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdConcept1(String idThesaurus, String idConcept);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdConcept2(String idThesaurus, String idConcept);

    @Modifying
    @Transactional
    void deleteAllByIdThesaurusAndIdConcept1AndIdConcept2AndRole(String idThesaurus, String idConcept1, String idConcept2, String role);

    @Modifying
    @Transactional
    @Query("UPDATE HierarchicalRelationship t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
        SELECT hr.*
        FROM hierarchical_relationship hr
        WHERE hr.id_thesaurus = :idThesaurus
        AND hr.id_concept1 = hr.id_concept2
        AND hr.role = :role
    """, nativeQuery = true)
    List<HierarchicalRelationship> getListLoopRelations(@Param("idThesaurus") String idThesaurus, @Param("role") String role);

    @Query(value = """
        SELECT hr.id_concept2 AS idConcept2, c.status AS status, hr.role AS role
        FROM hierarchical_relationship hr
            JOIN concept c ON c.id_concept = hr.id_concept1 AND c.id_thesaurus = hr.id_thesaurus
        WHERE hr.id_thesaurus = :idThesaurus
        AND hr.id_concept1 = :idConcept
        AND hr.role LIKE 'BT%'
    """, nativeQuery = true)
    List<BroaderRelationProjection> findBroaderConcepts(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT hr.id_concept2 AS idConcept2, hr.role AS role, c.status AS status
        FROM hierarchical_relationship hr
            JOIN concept c ON hr.id_concept2 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
        WHERE hr.id_thesaurus = :idThesaurus
        AND hr.id_concept1 = :idConcept
        AND hr.role = 'RT'
        AND c.status != 'CA'
    """, nativeQuery = true)
    List<RelatedRelationProjection> findRelatedConcepts(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query("""
        SELECT h FROM HierarchicalRelationship h
        WHERE h.idThesaurus = :idThesaurus 
        AND h.idConcept1 = :idConcept 
        AND h.role = 'BT'
    """)
    Optional<HierarchicalRelationship> findBtRelation(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept);

    @Query("""
        SELECT h FROM HierarchicalRelationship h
        WHERE h.idThesaurus = :idThesaurus 
        AND h.idConcept1 = :idConcept1 
        AND h.idConcept2 = :idConcept2 
        AND h.role = 'BT'
        AND h.idConcept1 IN (
            SELECT h2.idConcept2 FROM HierarchicalRelationship h2
            WHERE h2.idThesaurus = :idThesaurus 
            AND h2.idConcept1 = :idConcept2 
            AND h2.idConcept2 = :idConcept1 
            AND h2.role = 'BT'
        )
    """)
    Optional<HierarchicalRelationship> findLoopBtRelation(@Param("idThesaurus") String idThesaurus, @Param("idConcept1") String idConcept1,
                                                          @Param("idConcept2") String idConcept2);

    Optional<HierarchicalRelationship> findByIdThesaurusAndIdConcept1AndIdConcept2(String idThesaurus, String idConcept1, String idConcept2);

    @Query(value = """
        SELECT DISTINCT hr.id_concept1
        FROM hierarchical_relationship hr
        WHERE hr.id_thesaurus = :idThesaurus
        AND hr.role LIKE 'NT%'
        AND hr.id_concept1 NOT IN (SELECT hr2.id_concept2 FROM hierarchical_relationship hr2 WHERE hr2.id_thesaurus = :idThesaurus AND (hr2.role NOT LIKE 'BT%' AND hr2.role NOT LIKE 'RT%'))
    """, nativeQuery = true)
    List<String> findTopConceptsWithNTOnly(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT c.id_concept
        FROM concept c
        WHERE c.id_thesaurus = :idThesaurus
        AND c.id_concept NOT IN (SELECT DISTINCT hr.id_concept1 
                                 FROM hierarchical_relationship hr
                                 WHERE hr.id_thesaurus = :idThesaurus
                                 AND hr.role NOT LIKE 'RT%')
        """, nativeQuery = true)
    List<String> findIsolatedConcepts(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT hr.*
        FROM hierarchical_relationship hr
        WHERE hr.id_thesaurus = :idThesaurus
        AND hr.id_concept1 = :concept1
        AND hr.id_concept2 = :concept2
        AND (hr.role LIKE 'NT%' or hr.role LIKE 'BT%')
    """, nativeQuery = true)
    List<HierarchicalRelationship> findAllByIdThesaurusAndIdConcept1AndRole(@Param("idThesaurus") String idThesaurus,
                                                                            @Param("concept1") String concept1,
                                                                            @Param("concept2") String concept2);

    @Query(value = """
        SELECT hr.id_concept2 AS idConcept, hr.role AS role, c.id_ark AS idArk, c.id_handle AS idHandle, c.id_doi AS idDoi
        FROM hierarchical_relationship hr
            JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
        WHERE hr.id_thesaurus = :idThesaurus
            AND hr.id_concept1 = :idConcept
             AND c.status != 'CA'
    """, nativeQuery = true)
    List<NodeHieraRelationProjection> getRelationsWithIdentifiers(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    // Requête avec pagination (OFFSET / LIMIT)
    @Query(value = """
        SELECT hr.id_concept2, hr.role
        FROM hierarchical_relationship hr 
                JOIN concept c ON hr.id_concept2 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
        WHERE hr.id_thesaurus = :idThesaurus
            AND hr.id_concept1 = :idConcept
            AND hr.role LIKE 'NT%'
            AND c.status != 'CA'
        OFFSET :offset FETCH NEXT :step ROWS ONLY
    """, nativeQuery = true)
    List<NodeNTProjection> findNTByConceptWithPagination(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus,
            @Param("step") int step, @Param("offset") int offset);

    @Query(value = """
        SELECT hr.id_concept2, hr.role
        FROM hierarchical_relationship hr 
                JOIN concept c ON hr.id_concept2 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
        WHERE hr.id_thesaurus = :idThesaurus
          AND hr.id_concept1 = :idConcept
          AND hr.role LIKE 'NT%'
          AND c.status != 'CA'
    """, nativeQuery = true)
    List<NodeNTProjection> findNTByConceptNoLimit(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT hr.id_concept2 AS idConcept2, hr.role AS role
        FROM hierarchical_relationship hr
        JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
        WHERE hr.id_concept1 = :idConcept
          AND hr.id_thesaurus = :idThesaurus
          AND hr.role NOT IN ('BT', 'BTG', 'BTP', 'BTI', 'NT', 'NTG', 'NTP', 'NTI', 'RT')
          AND c.status != 'CA'
    """, nativeQuery = true)
    List<CustomRelationProjection> findCustomRelations(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT COUNT(hr.id_concept1)
        FROM hierarchical_relationship hr
        WHERE hr.id_thesaurus = :idThesaurus
          AND hr.id_concept1 = :idConcept
          AND hr.role LIKE 'BT%'
    """, nativeQuery = true)
    int countBroaderRelations(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT id_concept2
        FROM hierarchical_relationship
        WHERE id_thesaurus = :idThesaurus
          AND id_concept1 = :idConcept
          AND role LIKE 'BT%'
    """, nativeQuery = true)
    List<String> findIdsOfBroaderConcepts(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM hierarchical_relationship
        WHERE id_thesaurus = :idThesaurus
          AND id_concept1 = :idConcept1
          AND id_concept2 = :idConcept2
          AND role LIKE 'RT%'
    """, nativeQuery = true)
    boolean existsRelationRT(@Param("idConcept1") String idConcept1, @Param("idConcept2") String idConcept2, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT COUNT(*) > 0
        FROM hierarchical_relationship hr1
        WHERE hr1.id_concept1 = :idConcept1
          AND hr1.id_thesaurus = :idThesaurus
          AND hr1.role ILIKE 'BT%'
          AND hr1.id_concept2 IN (SELECT hr2.id_concept2
              FROM hierarchical_relationship hr2
              WHERE hr2.id_concept1 = :idConcept2
                AND hr2.id_thesaurus = :idThesaurus
                AND hr2.role ILIKE 'BT%')
    """, nativeQuery = true)
    boolean existsBrotherRelation(@Param("idConcept1") String idConcept1, @Param("idConcept2") String idConcept2,
                                  @Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT DISTINCT hr.id_concept2 AS idConcept, t.lexical_value AS preferredTerm
        FROM hierarchical_relationship hr
        JOIN preferred_term pt ON hr.id_concept2 = pt.id_concept AND hr.id_thesaurus = pt.id_thesaurus
        JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
        WHERE hr.id_thesaurus = :idThesaurus
          AND hr.id_concept1 = :idConcept
          AND hr.role LIKE 'NT%'
          AND t.lang = :idLang
        ORDER BY t.lexical_value
        """, nativeQuery = true)
    List<NodeTreeView> findChildrenWithPreferredTerm(@Param("idConcept") String idConcept, @Param("idLang") String idLang,
                                                     @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query(value = "UPDATE hierarchical_relationship SET id_thesaurus = :target WHERE id_concept1 = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurusByConcept1(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    @Modifying
    @Transactional
    @Query(value = "UPDATE hierarchical_relationship SET id_thesaurus = :target WHERE id_concept2 = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurusByConcept2(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    @Modifying
    @Transactional
    @Query(value = "UPDATE hierarchical_relationship SET role = :role WHERE id_concept1 = :idConcept1 AND id_concept2 = :idConcept2 AND id_thesaurus = :idThesaurus", nativeQuery = true)
    void updateRole(@Param("role") String role,
                    @Param("idConcept1") String idConcept1,
                    @Param("idConcept2") String idConcept2,
                    @Param("idThesaurus") String idThesaurus);
}
