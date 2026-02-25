package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.ConceptGroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;


public interface ConceptGroupRepository extends JpaRepository<ConceptGroup, Integer> {

    @Query("""
        SELECT cg
        FROM ConceptGroup cg
        JOIN ConceptGroupLabel cgl
            ON cgl.idGroup = cg.idGroup
           AND cgl.idThesaurus = cg.idThesaurus
        WHERE cg.idThesaurus = :idThesaurus
          AND cgl.lang = :lang
        ORDER BY LOWER(cgl.lexicalValue)
    """)
    List<ConceptGroup> findGroupsSortedByLexicalValue(
            @Param("idThesaurus") String idThesaurus,
            @Param("lang") String lang
    );


    @Modifying
    @Transactional
    @Query("UPDATE ConceptGroup cg SET cg.isPrivate = :isPrivate WHERE cg.idGroup = :idGroup AND cg.idThesaurus = :idThesaurus")
    void updateVisibility(@Param("idGroup") String idGroup, @Param("idThesaurus") String idThesaurus, @Param("isPrivate") boolean isPrivate);

    Optional<ConceptGroup> findByIdGroupAndIdThesaurus(String idGroup, String idThesaurus);

    Optional<ConceptGroup> findAllByIdThesaurusAndIdArk(String idThesaurus, String idArk);

    Optional<ConceptGroup> findByIdHandle(String idHandle);

    List<ConceptGroup> findAllByIdThesaurus(String idThesaurus);

    List<ConceptGroup> findByIdThesaurusAndNotation(String idThesaurus, String notation);

    @Query(value = "SELECT nextval('concept_group__id_seq')", nativeQuery = true)
    Long getNextConceptGroupSequence();

    @Query(value = "SELECT COUNT(*) FROM concept_group WHERE LOWER(idgroup) = LOWER(:idGroup)", nativeQuery = true)
    int countByIdGroupIgnoreCase(@Param("idGroup") String idGroup);

    @Modifying
    @Transactional
    void deleteByIdThesaurusAndIdGroup(String idThesaurus, String idGroup);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM concept_group WHERE idthesaurus = :idThesaurus", nativeQuery = true)
    void deleteByIdThesaurus(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT idthesaurus 
        FROM concept_group 
        WHERE REPLACE(id_ark, '-', '') = REPLACE(:arkId, '-', '')
        LIMIT 1
    """, nativeQuery = true)
    String findThesaurusIdByArkId(@Param("arkId") String arkId);

    @Query(value = """
        SELECT idgroup 
        FROM concept_group 
        WHERE idthesaurus = :idThesaurus
        AND idgroup NOT IN (SELECT id_group2 FROM relation_group WHERE relation = 'sub' AND id_thesaurus = :idThesaurus)
        ORDER BY notation ASC
        """, nativeQuery = true)
    List<String> findRootGroups(@Param("idThesaurus") String idThesaurus);


    @Query(value = """
        SELECT idgroup 
        FROM concept_group 
        WHERE idthesaurus = :idThesaurus
        AND private = false
        AND idgroup NOT IN (SELECT id_group2 FROM relation_group WHERE relation = 'sub' AND id_thesaurus = :idThesaurus)
        ORDER BY notation ASC
        """, nativeQuery = true)
    List<String> findRootGroupsPublicOnly(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT cg.idgroup, cg.id_ark, cg.id_handle, cg.id_doi
        FROM concept_group cg
            JOIN concept_group_concept cgc ON LOWER(cg.idgroup) = LOWER(cgc.idgroup)
        AND cg.idthesaurus = cgc.idthesaurus
        WHERE cgc.idconcept = :idConcept
        AND cgc.idthesaurus = :idThesaurus
    """, nativeQuery = true)
    List<Object[]> findGroupsOfConcept(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept);

    @Modifying
    @Transactional
    @Query(value = "UPDATE concept_group SET modified = now() WHERE idGroup = :idGroup AND idThesaurus = :idThesaurus", nativeQuery = true)
    void updateModifiedDate(String idGroup, String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE ConceptGroup cg SET cg.notation = '' WHERE cg.notation ilike 'null'")
    void cleanNotation();

    @Modifying
    @Transactional
    @Query("UPDATE ConceptGroup cg SET cg.idTypeCode = 'MT' WHERE cg.idTypeCode ilike 'null'")
    void cleanIdTypeCode();

    @Modifying
    @Transactional
    @Query("UPDATE ConceptGroup t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Transactional
    @Query(value = "UPDATE concept_group_concept SET idthesaurus = :target WHERE idconcept = :concept AND idthesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);
}
