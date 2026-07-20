package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.models.ConceptIdView;
import fr.cnrs.opentheso.models.NodeDeprecatedProjection;
import fr.cnrs.opentheso.models.TopConceptProjection;
import fr.cnrs.opentheso.models.concept.NodeUri;
import fr.cnrs.opentheso.models.nodes.NodeTree;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public interface ConceptRepository extends JpaRepository<Concept, Integer> {



    @Modifying
    @Transactional
    @Query(value = """
                INSERT INTO concept (
                    id_concept,
                    id_thesaurus,
                    id_ark,
                    notation,
                    top_concept,
                    creator,
                    concept_type,
                    status,
                    created,
                    modified
                )
                VALUES (
                    :idConcept,
                    :idThesaurus,
                    :idArk,
                    COALESCE(:notation, ''),
                    :topConcept,
                    COALESCE(:creator, -1),
                    COALESCE(:conceptType, 'concept'),
                    COALESCE(:status, 'D'),
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
            """, nativeQuery = true)
    void saveConcept(
            @Param("idConcept") String idConcept,
            @Param("idThesaurus") String idThesaurus,
            @Param("idArk") String idArk,
            @Param("notation") String notation,
            @Param("topConcept") boolean topConcept,
            @Param("creator") Integer creator,
            @Param("conceptType") String conceptType,
            @Param("status") String status
    );

    @Query("SELECT c.idArk, c.idConcept " +
            "FROM Concept c " +
            "WHERE c.idArk IN :arkIds AND c.idThesaurus = :idTheso")
    List<Object[]> findArkIds(@Param("arkIds") Set<String> arkIds,
                              @Param("idTheso") String idTheso);

    @Query("SELECT c.idConcept, c.idArk " +
            "FROM Concept c " +
            "WHERE c.idConcept IN :idConcepts AND c.idThesaurus = :idTheso")
    List<Object[]> findArkFromIdConcepts(@Param("idConcepts") Set<String> idConcepts,
                                         @Param("idTheso") String idTheso);

    // Optionnel : récupérer en batch pour éviter des milliers d'appels
    @Query("SELECT c.idConcept FROM Concept c WHERE c.idThesaurus = :idThesaurus AND c.idConcept IN :ids")
    List<String> findExistingIds(@Param("ids") Set<String> ids, @Param("idThesaurus") String idThesaurus);



    List<Concept> findByIdConcept(String idConcept);

    List<Concept> findAllByIdThesaurusAndNotationLike(String idThesaurus, String notation);

    boolean existsByIdThesaurusAndNotation(String idThesaurus, String notation);

    Optional<Concept> findByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    List<Concept> findAllByIdThesaurusAndTopConceptAndStatusNotLike(String idThesaurus, boolean isTopConcept, String status);

    @Query(value = """
                SELECT count(c.id_concept) FROM concept c WHERE c.id_thesaurus = :idThesaurus AND c.status not in ('CA', 'DEP', 'dep')
            """, nativeQuery = true)
    int countConcepts(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT count(c.id_concept) FROM concept c WHERE c.id_thesaurus = :idThesaurus AND status = 'CA'
            """, nativeQuery = true)
    int countCandidate(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
               SELECT count(c.id_concept) FROM concept c WHERE c.id_thesaurus = :idThesaurus AND LOWER(status) = LOWER('dep')
            """, nativeQuery = true)
    int countConceptDeprecated(@Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE Concept c SET c.topConcept = :status WHERE c.idConcept = :idConcept AND c.idThesaurus = :idThesaurus")
    void setTopConceptTag(@Param("status") boolean status, @Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE Concept c SET c.idArk = :idArk, c.modified = :now WHERE c.idConcept = :idConcept AND c.idThesaurus = :idThesaurus")
    void setIdArk(@Param("idArk") String idArk, @Param("now") Date now, @Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE Concept c SET c.conceptType = :type, c.modified = :now WHERE c.idConcept = :idConcept AND c.idThesaurus = :idThesaurus")
    void setConceptType(@Param("type") String type, @Param("now") Date now, @Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);


    @Modifying
    @Transactional
    @Query("UPDATE Concept c SET c.gps = :status WHERE c.idConcept = :idConcept AND c.idThesaurus = :idThesaurus")
    int setGpsTag(@Param("status") boolean status, @Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE Concept c SET c.status = :status WHERE c.idConcept = :idConcept AND c.idThesaurus = :idThesaurus")
    int setStatus(@Param("status") String status, @Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    List<Concept> findAllByIdThesaurusAndStatus(String idThesaurus, String status);

    List<Concept> findAllByIdThesaurusAndStatusNot(String idThesaurus, String status);

    List<Concept> findAllByIdThesaurus(String idThesaurus);

    @Query(value = "SELECT nextval('concept__id_seq')", nativeQuery = true)
    Long getNextConceptNumericId();

    @Modifying
    @Transactional
    @Query("UPDATE Concept c SET c.notation = '' WHERE c.notation ilike 'null'")
    void cleanConcept();

    @Modifying
    @Transactional
    @Query("UPDATE Concept c SET c.notation = :notation WHERE c.idConcept = :idConcept and c.idThesaurus = :idThesaurus")
    int updateNotation(String idConcept, String idThesaurus, String notation);

  /*  @Modifying
    @Transactional
    void deleteAllByIdThesaurus(String thesaurus);
*/
    @Modifying
    @Query("DELETE FROM Concept c WHERE c.idThesaurus = :idThesaurus")
    void deleteAllByIdThesaurus(@Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query(value = """
                DELETE FROM concept
                WHERE id_thesaurus = :thesaurus
                  AND id_concept = :idConcept
            """, nativeQuery = true)
    void deleteAllByIdThesaurusAndIdConcept(@Param("thesaurus") String thesaurus,
                                            @Param("idConcept") String idConcept);

    @Modifying
    @Transactional
    @Query(value = """
                UPDATE concept 
                SET modified = CURRENT_DATE,
                    contributor = :contributor
                WHERE id_thesaurus = :thesaurus
                  AND id_concept = :idConcept
            """, nativeQuery = true)
    int updateDateOfConcept(@Param("thesaurus") String thesaurus,
                            @Param("idConcept") String idConcept,
                            @Param("contributor") int contributor);

    @Modifying
    @Transactional
    @Query("UPDATE Concept t SET t.idThesaurus = :newIdThesaurus WHERE t.idThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Query(value = """
            SELECT DISTINCT c.id_concept, c.notation
            FROM concept c JOIN concept_group_concept gc ON c.id_concept = gc.idconcept AND c.id_thesaurus = gc.idthesaurus
            WHERE c.id_thesaurus = :idThesaurus
                AND LOWER(gc.idgroup) = LOWER(:idGroup)
                AND c.top_concept IS TRUE
                AND c.status != 'CA'
                LIMIT 2001
            """, nativeQuery = true)
    List<Object[]> findTopConceptsByGroup(@Param("idThesaurus") String idThesaurus, @Param("idGroup") String idGroup);

    @Query(value = """
                SELECT DISTINCT c.id_concept, c.notation
                FROM concept c JOIN concept_group_concept gc ON c.id_concept = gc.idconcept AND c.id_thesaurus = gc.idthesaurus
                WHERE c.id_thesaurus = :idThesaurus
                AND LOWER(gc.idgroup) = LOWER(:idGroup)
                AND c.status != 'CA'
                LIMIT 2001
            """, nativeQuery = true)
    List<Object[]> findConceptsByGroup(@Param("idThesaurus") String idThesaurus, @Param("idGroup") String idGroup);

    @Query(value = """
                SELECT concept.id_concept
                FROM concept
                JOIN preferred_term ON concept.id_concept = preferred_term.id_concept AND concept.id_thesaurus = preferred_term.id_thesaurus
                JOIN non_preferred_term ON preferred_term.id_term = non_preferred_term.id_term AND preferred_term.id_thesaurus = non_preferred_term.id_thesaurus
                WHERE non_preferred_term.id_thesaurus = :idTheso
                AND non_preferred_term.lang = :idLang
                AND LOWER(non_preferred_term.lexical_value) = LOWER(:label)
                AND LOWER(concept.status) != LOWER('dep')
                LIMIT 1
            """, nativeQuery = true)
    Optional<String> findConceptIdFromAltLabel(@Param("idTheso") String idTheso, @Param("label") String label, @Param("idLang") String idLang);

    @Query(value = """
                SELECT c.id_concept
                FROM concept c
                        JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                        JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE t.id_thesaurus = :idTheso
                AND t.lang = :idLang
                AND LOWER(t.lexical_value) = LOWER(:label)
                AND c.status NOT IN ('DEP', 'dep', 'CA')
                LIMIT 1
            """, nativeQuery = true)
    Optional<String> findConceptIdFromLabel(@Param("idTheso") String idTheso, @Param("label") String label, @Param("idLang") String idLang);

    @Query("""
                 SELECT c.modified FROM Concept c\s
                 WHERE c.idThesaurus = :idThesaurus\s
                 AND c.status <> 'CA'\s
                 AND c.modified IS NOT NULL\s
                 ORDER BY c.modified DESC
            \s""")
    List<Date> findLastModifiedDates(@Param("idThesaurus") String idThesaurus, PageRequest pageable);

    @Query(value = """
                SELECT c.id_concept, t.lexical_value
                FROM concept c
                    JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                    JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.id_thesaurus = :idThesaurus
                    AND t.lang = :lang
                    AND c.status != 'CA'
                    AND c.modified IS NOT NULL
                ORDER BY c.modified DESC, t.lexical_value
                LIMIT 10
            """, nativeQuery = true)
    List<Object[]> findLastModifiedConcepts(@Param("idThesaurus") String idThesaurus, @Param("lang") String lang);

    @Query("""
                SELECT c.idHandle FROM Concept c
                WHERE c.idThesaurus = :idThesaurus AND c.idHandle IS NOT NULL AND c.idHandle <> ''
            """)
    List<String> findAllNonEmptyIdHandleByThesaurus(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
            SELECT c.id_concept AS idConcept, c.notation AS notation, c.status AS status
            FROM concept c
            WHERE c.top_concept = true
              AND c.status != 'CA'
              AND c.id_thesaurus = :idThesaurus
            """, nativeQuery = true)
    List<TopConceptProjection> findTopConcepts(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT c.id_concept AS idConcept, c.notation AS notation, c.status AS status
                FROM concept c
                LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept
                LEFT JOIN concept_group cg ON cgc.idgroup = cg.idgroup
                WHERE c.top_concept = true
                  AND c.status != 'CA'
                  AND c.id_thesaurus = :idThesaurus
                GROUP BY c.id_concept, c.notation, c.status
                HAVING BOOL_OR(cg.private IS NULL OR cg.private = false)
            """, nativeQuery = true)
    List<TopConceptProjection> findTopConceptsPrivate(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT COUNT(h.id_concept2)
                FROM hierarchical_relationship h, concept c
                WHERE h.id_concept2 = c.id_concept 
                      AND h.id_thesaurus = c.id_thesaurus
                      AND h.id_thesaurus = :idThesaurus
                      AND h.id_concept1 = :idConcept
                      AND h.role LIKE 'NT%'
                      AND c.status != 'CA'
            """, nativeQuery = true)
    int countChildren(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept);

    @Query(value = """
                SELECT c.id_concept as idConcept, c.modified as modified, u.username as username, t.lexical_value as lexicalValue
                FROM concept c
                    JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                    JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                    JOIN users u ON c.contributor = u.id_user
                WHERE c.id_thesaurus = :idThesaurus
                    AND t.lang = :idLang
                    AND LOWER(c.status) = LOWER('dep')
                ORDER BY unaccent(lower(t.lexical_value))
            """, nativeQuery = true)
    List<NodeDeprecatedProjection> findAllDeprecatedConcepts(@Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang);

    @Query("""
                SELECT c.idArk
                FROM Concept c
                    JOIN HierarchicalRelationship h ON h.idConcept2 = c.idConcept AND h.idThesaurus = c.idThesaurus
                WHERE h.idThesaurus = :idThesaurus
                    AND h.idConcept1 = :idConcept
                AND h.role LIKE 'NT%'
                AND c.status <> 'CA'
            """)
    List<String> findArkIdsOfChildren(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept);

    @Transactional
    @Modifying
    @Query(value = """
                UPDATE concept
                SET id_concept = :newIdConcept
                WHERE id_concept = :oldIdConcept AND id_thesaurus = :idThesaurus
            """, nativeQuery = true)
    void updateConceptId(@Param("idThesaurus") String idThesaurus, @Param("oldIdConcept") String oldIdConcept, @Param("newIdConcept") String newIdConcept);

    @Query("SELECT c.idConcept FROM Concept c WHERE LOWER(c.idHandle) = LOWER(:handleId)")
    Optional<String> findConceptIdByHandleIgnoreCase(@Param("handleId") String handleId);

    @Query(value = """
                 SELECT c.id_concept\s
                 FROM concept c\s
                 WHERE c.id_thesaurus = :idThesaurus\s
                 AND REPLACE(c.id_ark, '-', '') ILIKE REPLACE(:arkId, '-', '')
            \s""", nativeQuery = true)
    Optional<String> findConceptIdByArkIgnoreCase(@Param("arkId") String arkId, @Param("idThesaurus") String idThesaurus);

    @Query(value = "SELECT id_concept FROM concept WHERE id_thesaurus = :idThesaurus AND (id_ark IS NULL OR id_ark = '') AND status != 'CA'",
            nativeQuery = true)
    List<String> findAllIdConceptsWithoutArk(@Param("idThesaurus") String idThesaurus);

    @Query("""
                SELECT new fr.cnrs.opentheso.models.concept.NodeUri(COALESCE(c.idArk, ''), COALESCE(c.idHandle, ''), COALESCE(c.idDoi, ''), c.idConcept)
                FROM Concept c
                WHERE c.idThesaurus = :idThesaurus
                    AND c.topConcept = true
                    AND c.status <> 'CA'
            """)
    List<NodeUri> findAllTopConceptsWithUris(@Param("idThesaurus") String idThesaurus);

    @Query("""
                SELECT c.idConcept
                FROM Concept c
                WHERE c.idThesaurus = :idThesaurus
                  AND c.status <> 'CA'
                  AND c.modified >= :startDate
            """)
    List<String> findConceptIdsModifiedSince(@Param("idThesaurus") String idThesaurus, @Param("startDate") LocalDate startDate);


    @Query("""
    SELECT c.idConcept
    FROM Concept c
    WHERE c.idThesaurus = :idThesaurus
      AND c.status <> 'CA'
      AND c.modified >= :startDate
""")
    List<String> findConceptsModifiedSince(
            @Param("idThesaurus") String idThesaurus,
            @Param("startDate") Date startDate
    );


    boolean existsByIdConceptAndIdThesaurus(String idConcept, String idThesaurus);

    @Query(value = """
            SELECT c.id_concept AS idConcept
            FROM concept c
            JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept AND c.id_thesaurus = cgc.idthesaurus
            WHERE c.id_thesaurus = :idThesaurus
              AND c.status != 'CA'
              AND LOWER(cgc.idgroup) IN (:groupIds)
            """, nativeQuery = true)
    List<ConceptIdView> findAllByThesaurusAndGroups(@Param("idThesaurus") String idThesaurus, @Param("groupIds") List<String> groupIds);

    @Query("SELECT new fr.cnrs.opentheso.models.concept.NodeUri(c.idArk, c.idHandle, c.idDoi, c.idConcept) " +
            "FROM Concept c JOIN ConceptGroupConcept cgc ON c.idConcept = cgc.idConcept AND c.idThesaurus = cgc.idThesaurus " +
            "WHERE c.idThesaurus = :idThesaurus AND LOWER(cgc.idGroup) = LOWER(:idGroup) AND c.status <> 'CA'")
    List<NodeUri> findConceptsByThesaurusAndGroup(@Param("idThesaurus") String idThesaurus, @Param("idGroup") String idGroup);

    @Query("SELECT c.idConcept FROM Concept c " +
            "JOIN ConceptGroupConcept cgc ON c.idConcept = cgc.idConcept AND c.idThesaurus = cgc.idThesaurus " +
            "WHERE c.idThesaurus = :idThesaurus " +
            "AND LOWER(cgc.idGroup) = LOWER(:idGroup) " +
            "AND c.status != 'CA'")
    List<String> findAllConceptIdsByGroup(@Param("idThesaurus") String idThesaurus, @Param("idGroup") String idGroup);

    @Query("SELECT c.idConcept FROM Concept c " +
            "WHERE c.idThesaurus = :idThesaurus " +
            "AND (c.idHandle IS NULL OR c.idHandle = '')")
    List<String> findAllIdsWithoutHandle(@Param("idThesaurus") String idThesaurus);

    @Query("SELECT c.idConcept FROM Concept c " +
            "WHERE c.idThesaurus = :idThesaurus " +
            "AND c.topConcept = true " +
            "AND c.status <> 'CA'")
    List<String> findAllTopConceptIdsByThesaurus(@Param("idThesaurus") String idThesaurus);

    @Query(
            value = "SELECT c.id_thesaurus FROM concept c WHERE REPLACE(c.id_ark, '-', '') = REPLACE(:arkId, '-', '') LIMIT 1",
            nativeQuery = true
    )
    String findIdThesaurusListByArkId(@Param("arkId") String arkId);

    @Query("""
                SELECT new fr.cnrs.opentheso.models.nodes.NodeTree(c.idConcept, t.lexicalValue)
                FROM Concept c
                    JOIN PreferredTerm pt ON pt.idConcept = c.idConcept AND pt.idThesaurus = c.idThesaurus
                    JOIN Term t ON t.idTerm = pt.idTerm AND t.idThesaurus = pt.idThesaurus
                WHERE c.idThesaurus = :idThesaurus
                    AND c.topConcept = true
                AND c.status != 'CA'
                AND t.lang = :idLang
                ORDER BY t.lexicalValue
            """)
    List<NodeTree> findTopConceptsWithTermByThesaurusAndLang(@Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang);

    Optional<Concept> findByIdHandle(String idHandle);

    @Modifying
    @Query(value = "UPDATE concept SET id_thesaurus = :target WHERE id_concept = :concept AND id_thesaurus = :from", nativeQuery = true)
    void updateThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);

    @Procedure(procedureName = "opentheso_add_new_concept")
    void addNewConcept(String idTheso, String idConcept, Integer idUser, String status, String conceptType, String notation,
                       String arkId, Boolean isTopConcept, String handle, String doi, String prefLabels, String relations,
                       String customRelations, String notes, String nonPrefLabels, String alignments, String images, String replacedBy,
                       Boolean hasGps, String gps, Date created, Date modified, String dcterms);


    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = "UPDATE concept SET top_concept = :isTopConcept, modified = CURRENT_TIMESTAMP " +
                    "WHERE id_concept = :idConcept AND id_thesaurus = :idThesaurus",
            nativeQuery = true
    )
    int updateTopConcept(@Param("idConcept") String idConcept,
                         @Param("idThesaurus") String idThesaurus,
                         @Param("isTopConcept") boolean isTopConcept);
}
