package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.Alignement;
import fr.cnrs.opentheso.models.NodeAlignmentProjection;
import fr.cnrs.opentheso.models.NodeIdValueProjection;
import fr.cnrs.opentheso.models.NodeSelectedAlignmentProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface AlignementRepository extends JpaRepository<Alignement, Integer> {

    @Query(value = """
        SELECT * FROM alignement WHERE internal_id_thesaurus = :idThesaurus AND internal_id_concept NOT IN (SELECT idconcept FROM concept_group_concept WHERE idthesaurus = :idThesaurus)
    """, nativeQuery = true)
    List<Alignement> findAlignementsNotInConceptGroup(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT a.*
        FROM concept_group_concept cgc, alignement a
        WHERE cgc.idconcept = a.internal_id_concept
        AND cgc.idthesaurus = a.internal_id_thesaurus
        AND LOWER(cgc.idgroup) = LOWER(:idGroup)
        AND cgc.idthesaurus = :idThesaurus
    """, nativeQuery = true)
    List<Alignement> findAlignementsByGroupAndThesaurus(@Param("idGroup") String idGroup, @Param("idThesaurus") String idThesaurus);

    Optional<Alignement> findByInternalIdThesaurusAndInternalIdConceptAndId(String internalIdThesaurus, String internalIdConcept, Integer id);

    boolean existsByInternalIdThesaurusAndInternalIdConceptAndUriTarget(String internalIdThesaurus, String internalIdConcept, String uriTarget);

    @Transactional
    @Modifying
    @Query("DELETE FROM Alignement a WHERE a.id = :idAlignment AND a.internalIdThesaurus = :idThesaurus")
    int deleteByIdAndThesaurus(@Param("idAlignment") int idAlignment, @Param("idThesaurus") String idThesaurus);

    @Transactional
    @Modifying
    @Query("DELETE FROM Alignement a WHERE a.uriTarget = :uri AND a.internalIdThesaurus = :idThesaurus AND a.internalIdConcept = :idConcept")
    int deleteByUriAndConceptAndThesaurus(@Param("uri") String uri, @Param("idConcept") String idConcept,
                                          @Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("DELETE FROM Alignement a WHERE a.internalIdThesaurus = :idThesaurus")
    void deleteByThesaurus(@Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("DELETE FROM Alignement a WHERE a.internalIdConcept = :idConcept AND a.internalIdThesaurus = :idThesaurus AND a.alignementType.id = :typeId")
    void deleteByConceptThesaurusAndType(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus,
                                         @Param("typeId") int typeId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Alignement a WHERE a.internalIdConcept = :idConcept AND a.internalIdThesaurus = :idThesaurus")
    void deleteByConceptAndThesaurus(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM Alignement a
        WHERE a.internalIdConcept = :idConcept
            AND a.internalIdThesaurus = :idThesaurus
            AND a.alignementType.id = :typeId
            AND a.uriTarget = :uriTarget
    """)
    boolean existsByConceptThesaurusTypeAndUri(@Param("idThesaurus") String idThesaurus, @Param("idConcept") String idConcept,
                                               @Param("typeId") int alignementTypeId, @Param("uriTarget") String uriTarget);

    @Query(value = """
        SELECT a.id, a.created, a.modified, a.author, a.thesaurus_target, a.concept_target, a.uri_target,
            a.alignement_id_type, a.internal_id_thesaurus, a.internal_id_concept, a.id_alignement_source, t.label, t.label_skos, a.url_available
        FROM alignement a JOIN alignement_type t ON a.alignement_id_type = t.id
        WHERE a.internal_id_concept = :idConcept
        AND a.internal_id_thesaurus = :idThesaurus
        ORDER BY a.id ASC
    """, nativeQuery = true)
    List<NodeAlignmentProjection> findAllAlignmentsByConceptAndThesaurus(@Param("idConcept") String idConcept,
                                                                         @Param("idThesaurus") String idThesaurus);


    @Query(value = """
        SELECT a.source, a.description, tas.id_alignement_source 
        FROM thesaurus_alignement_source tas
        JOIN alignement_source a ON a.id = tas.id_alignement_source
        WHERE tas.id_thesaurus = :idThesaurus
    """, nativeQuery = true)
    List<NodeSelectedAlignmentProjection> findSelectedAlignmentsByThesaurus(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
        SELECT internal_id_concept, uri_target
        FROM alignement
        WHERE internal_id_thesaurus = :idTheso
        AND alignement_id_type = 1
        AND (uri_target ILIKE CONCAT('%ontome.net/ontology/', :cidocClass) OR uri_target ILIKE CONCAT('%ontome.net/ontology/c', :cidocClass) OR uri_target ILIKE CONCAT('%ontome.net/class/', :cidocClass))
    """, nativeQuery = true)
    List<NodeIdValueProjection> findLinkedConceptsWithOntome(@Param("idTheso") String idTheso, @Param("cidocClass") String cidocClass);

    @Query(value = """
        SELECT internal_id_concept, uri_target
        FROM alignement
        WHERE internal_id_thesaurus = :idThesaurus
        AND alignement_id_type = 1
        AND (uri_target ILIKE '%ontome.net/ontology%' OR uri_target ILIKE '%ontome.net/class%')
    """, nativeQuery = true)
    List<NodeIdValueProjection> findAllLinkedConceptsWithOntome(@Param("idThesaurus") String idThesaurus);

    @Modifying
    @Transactional
    @Query("UPDATE Alignement t SET t.internalIdThesaurus = :newIdThesaurus WHERE t.internalIdThesaurus = :oldIdThesaurus")
    void updateThesaurusId(@Param("newIdThesaurus") String newIdThesaurus, @Param("oldIdThesaurus") String oldIdThesaurus);

    @Modifying
    @Query(value = "UPDATE alignement SET internal_id_thesaurus = :target WHERE internal_id_concept = :concept AND internal_id_thesaurus = :from", nativeQuery = true)
    void updateInternalThesaurus(@Param("concept") String concept, @Param("from") String from, @Param("target") String target);
}
