package fr.cnrs.opentheso.v2.concept.write.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class ConceptLifecycleWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<ConceptSnapshot> loadConceptSnapshot(String thesaurusId, String conceptId) {
        return entityManager.createNativeQuery("""
                        SELECT id_concept, id_thesaurus, id_ark, status, notation, top_concept
                        FROM concept
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        LIMIT 1
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getResultStream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new ConceptSnapshot(
                            (String) values[0],
                            (String) values[1],
                            values[2] != null ? (String) values[2] : "",
                            values[3] != null ? (String) values[3] : "",
                            values[4] != null ? (String) values[4] : "",
                            values[5] != null && (Boolean) values[5]
                    );
                })
                .findFirst();
    }

    @Transactional
    public boolean updateConceptStatus(String thesaurusId, String conceptId, String status) {
        return entityManager.createNativeQuery("""
                        UPDATE concept
                        SET status = :status
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        """)
                .setParameter("status", status)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate() > 0;
    }

    @Transactional
    public void insertConceptHistory(ConceptSnapshot concept, int userId) {
        insertConceptHistory(concept, userId, "");
    }

    @Transactional
    public void insertConceptHistory(ConceptSnapshot concept, int userId, String idGroup) {
        entityManager.createNativeQuery("""
                        INSERT INTO concept_historique (
                            id_concept, id_thesaurus, id_ark, modified, status,
                            notation, top_concept, id_group, id_user
                        )
                        VALUES (
                            :conceptId, :thesaurusId, :idArk, :modified, :status,
                            :notation, :topConcept, :idGroup, :userId
                        )
                        """)
                .setParameter("conceptId", concept.conceptId())
                .setParameter("thesaurusId", concept.thesaurusId())
                .setParameter("idArk", concept.idArk())
                .setParameter("modified", new Date())
                .setParameter("status", concept.status())
                .setParameter("notation", concept.notation())
                .setParameter("topConcept", concept.topConcept())
                .setParameter("idGroup", idGroup == null ? "" : idGroup)
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Transactional
    public void insertReplacedBy(
            String deprecatedConceptId,
            String replacementConceptId,
            String thesaurusId,
            int userId
    ) {
        entityManager.createNativeQuery("""
                        INSERT INTO concept_replacedby (
                            id_concept1, id_concept2, id_thesaurus, modified, id_user
                        )
                        VALUES (
                            :deprecatedConceptId, :replacementConceptId, :thesaurusId, :modified, :userId
                        )
                        """)
                .setParameter("deprecatedConceptId", deprecatedConceptId)
                .setParameter("replacementConceptId", replacementConceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("modified", new Date())
                .setParameter("userId", userId)
                .executeUpdate();
    }

    @Transactional
    public void deleteReplacedBy(
            String deprecatedConceptId,
            String replacementConceptId,
            String thesaurusId
    ) {
        entityManager.createNativeQuery("""
                        DELETE FROM concept_replacedby
                        WHERE id_concept1 = :deprecatedConceptId
                          AND id_concept2 = :replacementConceptId
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("deprecatedConceptId", deprecatedConceptId)
                .setParameter("replacementConceptId", replacementConceptId)
                .setParameter("thesaurusId", thesaurusId)
                .executeUpdate();
    }

    @Transactional
    public void deleteAllReplacedByForConcept(String deprecatedConceptId, String thesaurusId) {
        entityManager.createNativeQuery("""
                        DELETE FROM concept_replacedby
                        WHERE id_concept1 = :deprecatedConceptId
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("deprecatedConceptId", deprecatedConceptId)
                .setParameter("thesaurusId", thesaurusId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<String> listReplacementConceptIds(String deprecatedConceptId, String thesaurusId) {
        return entityManager.createNativeQuery("""
                        SELECT id_concept2
                        FROM concept_replacedby
                        WHERE id_concept1 = :deprecatedConceptId
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("deprecatedConceptId", deprecatedConceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public boolean isTopConcept(String thesaurusId, String conceptId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT top_concept
                        FROM concept
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        LIMIT 1
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getResultStream()
                .findFirst()
                .map(value -> (Boolean) value)
                .orElse(false));
    }

    @Transactional
    public boolean setTopConcept(String thesaurusId, String conceptId, boolean topConcept) {
        return entityManager.createNativeQuery("""
                        UPDATE concept
                        SET top_concept = :topConcept
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        """)
                .setParameter("topConcept", topConcept)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate() > 0;
    }

    @Transactional
    public boolean clearArkId(String thesaurusId, String conceptId) {
        return entityManager.createNativeQuery("""
                        UPDATE concept
                        SET id_ark = ''
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate() > 0;
    }
}
