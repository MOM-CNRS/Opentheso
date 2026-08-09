package fr.cnrs.opentheso.v2.concept.write.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class ConceptDeletionWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<String> findPreferredTermId(String thesaurusId, String conceptId) {
        return entityManager.createNativeQuery("""
                        SELECT id_term
                        FROM preferred_term
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        LIMIT 1
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getResultStream()
                .map(String.class::cast)
                .findFirst();
    }

    @Transactional
    public void deleteConcept(String thesaurusId, String conceptId) {
        findPreferredTermId(thesaurusId, conceptId).ifPresent(idTerm -> deleteTerm(thesaurusId, idTerm));
        deleteAllRelations(thesaurusId, conceptId);
        deleteNotes(thesaurusId, conceptId);
        deleteAlignments(thesaurusId, conceptId);
        deleteFacets(thesaurusId, conceptId);
        deleteGroupLinks(thesaurusId, conceptId);
        deleteGps(thesaurusId, conceptId);
        deleteExternalImages(thesaurusId, conceptId);
        deleteExternalResources(thesaurusId, conceptId);
        deletePropositionDetails(thesaurusId, conceptId);
        deletePropositionModifications(thesaurusId, conceptId);
        deletePropositions(thesaurusId, conceptId);
        deleteReplacedByLinks(thesaurusId, conceptId);
        deleteConceptDcTerms(thesaurusId, conceptId);
        deleteConceptRow(thesaurusId, conceptId);
    }

    private void deleteTerm(String thesaurusId, String idTerm) {
        entityManager.createNativeQuery("""
                        DELETE FROM note
                        WHERE id_thesaurus = :thesaurusId AND id_term = :idTerm
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("idTerm", idTerm)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM non_preferred_term
                        WHERE id_thesaurus = :thesaurusId AND id_term = :idTerm
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("idTerm", idTerm)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM preferred_term
                        WHERE id_thesaurus = :thesaurusId AND id_term = :idTerm
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("idTerm", idTerm)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        DELETE FROM term
                        WHERE id_thesaurus = :thesaurusId AND id_term = :idTerm
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("idTerm", idTerm)
                .executeUpdate();
    }

    private void deleteAllRelations(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM hierarchical_relationship
                        WHERE id_thesaurus = :thesaurusId
                          AND (id_concept1 = :conceptId OR id_concept2 = :conceptId)
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteNotes(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM note
                        WHERE id_thesaurus = :thesaurusId
                          AND (identifier = :conceptId OR id_concept = :conceptId)
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteAlignments(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM alignement
                        WHERE internal_id_thesaurus = :thesaurusId
                          AND internal_id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteConceptRow(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM concept
                        WHERE id_thesaurus = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteFacets(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM concept_facet
                        WHERE id_thesaurus = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteGroupLinks(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM concept_group_concept
                        WHERE idthesaurus = :thesaurusId AND idconcept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteGps(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM gps
                        WHERE id_theso = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteExternalImages(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM external_images
                        WHERE id_thesaurus = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteExternalResources(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM external_resources
                        WHERE id_thesaurus = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deletePropositionDetails(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM proposition_modification_detail
                        WHERE id_proposition IN (
                            SELECT id
                            FROM proposition_modification
                            WHERE id_theso = :thesaurusId
                              AND id_concept = :conceptId
                        )
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deletePropositionModifications(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM proposition_modification
                        WHERE id_theso = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deletePropositions(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM proposition
                        WHERE id_thesaurus = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteReplacedByLinks(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM concept_replacedby
                        WHERE id_thesaurus = :thesaurusId
                          AND (id_concept1 = :conceptId OR id_concept2 = :conceptId)
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    private void deleteConceptDcTerms(String thesaurusId, String conceptId) {
        entityManager.createNativeQuery("""
                        DELETE FROM concept_dcterms
                        WHERE id_thesaurus = :thesaurusId AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }
}
