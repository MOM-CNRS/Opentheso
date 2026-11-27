package fr.cnrs.opentheso.concept.write.persistence;

import fr.cnrs.opentheso.shared.repository.NativeQueryParams;
import fr.cnrs.opentheso.concept.history.ConceptChangeLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class ConceptRelationWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Journal unique des modifications (concept_change_log), qui remplace les anciennes tables *_historique. */
    @org.springframework.beans.factory.annotation.Autowired
    private ConceptChangeLog changeLog;

    @Transactional
    public void addHierarchicalLink(String conceptId1, String conceptId2, String thesaurusId, String role, int userId) {
        insertRelationHistory(conceptId1, conceptId2, thesaurusId, role, userId, "ADD");
        insertRelationship(conceptId1, conceptId2, thesaurusId, role);
    }

    public int countBroaderRelations(String conceptId, String thesaurusId) {
        Number count = (Number) entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM hierarchical_relationship
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept1 = :conceptId
                          AND role LIKE 'BT%'
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .getSingleResult();
        return count.intValue();
    }

    @Transactional
    public void addBroaderRelation(String narrowerConceptId, String broaderConceptId, String thesaurusId, int userId) {
        insertRelationHistory(narrowerConceptId, broaderConceptId, thesaurusId, "BT", userId, "ADD");
        insertRelationship(narrowerConceptId, broaderConceptId, thesaurusId, "BT");
        insertRelationship(broaderConceptId, narrowerConceptId, thesaurusId, "NT");
    }

    @Transactional
    public void addNarrowerRelation(String broaderConceptId, String narrowerConceptId, String thesaurusId, int userId) {
        insertRelationHistory(broaderConceptId, narrowerConceptId, thesaurusId, "NT", userId, "ADD");
        insertRelationship(broaderConceptId, narrowerConceptId, thesaurusId, "NT");
        insertRelationship(narrowerConceptId, broaderConceptId, thesaurusId, "BT");
    }

    @Transactional
    public void deleteBroaderRelation(String narrowerConceptId, String broaderConceptId, String thesaurusId, int userId) {
        insertRelationHistory(narrowerConceptId, broaderConceptId, thesaurusId, "BT", userId, "DEL");
        deleteHierarchicalPair(narrowerConceptId, broaderConceptId, thesaurusId);
    }

    @Transactional
    public void deleteNarrowerRelation(String broaderConceptId, String narrowerConceptId, String thesaurusId, int userId) {
        insertRelationHistory(broaderConceptId, narrowerConceptId, thesaurusId, "RT", userId, "DELETE");
        deleteHierarchicalPair(broaderConceptId, narrowerConceptId, thesaurusId);
    }

    @Transactional
    public boolean addRelatedRelation(String conceptId1, String conceptId2, String thesaurusId, int userId) {
        try {
            insertRelationHistory(conceptId1, conceptId2, thesaurusId, "RT", userId, "ADD");
            insertRelationship(conceptId1, conceptId2, thesaurusId, "RT");
            insertRelationship(conceptId2, conceptId1, thesaurusId, "RT");
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Transactional
    public void deleteRelatedRelation(String conceptId1, String conceptId2, String thesaurusId, int userId) {
        insertRelationHistory(conceptId1, conceptId2, thesaurusId, "RT", userId, "DEL");
        deleteRelationship(conceptId1, conceptId2, thesaurusId, "RT");
        deleteRelationship(conceptId2, conceptId1, thesaurusId, "RT");
    }

    @Transactional
    public void updateRelationRoles(
            String conceptId1,
            String conceptId2,
            String thesaurusId,
            String directRole,
            String inverseRole,
            int userId
    ) {
        entityManager.createNativeQuery("""
                        UPDATE hierarchical_relationship
                        SET role = :role
                        WHERE id_concept1 = :conceptId1
                          AND id_concept2 = :conceptId2
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("role", directRole)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        UPDATE hierarchical_relationship
                        SET role = :role
                        WHERE id_concept1 = :conceptId2
                          AND id_concept2 = :conceptId1
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("role", inverseRole)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
        insertRelationHistory(conceptId1, conceptId2, thesaurusId, directRole, userId, "UPDATE");
    }

    public boolean hasBroaderRelation(String conceptId, String thesaurusId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM hierarchical_relationship
                            WHERE id_thesaurus = :thesaurusId
                              AND id_concept1 = :conceptId
                              AND role LIKE 'BT%'
                        )
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .getSingleResult());
    }

    public boolean hasHierarchicalRelation(String conceptId1, String conceptId2, String thesaurusId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM hierarchical_relationship
                            WHERE id_thesaurus = :thesaurusId
                              AND id_concept1 = :conceptId1
                              AND id_concept2 = :conceptId2
                              AND (role LIKE 'NT%' OR role LIKE 'BT%')
                        )
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
                .getSingleResult());
    }

    public boolean hasRelatedRelation(String conceptId1, String conceptId2, String thesaurusId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM hierarchical_relationship
                            WHERE id_thesaurus = :thesaurusId
                              AND id_concept1 = :conceptId1
                              AND id_concept2 = :conceptId2
                              AND role LIKE 'RT%'
                        )
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
                .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    public List<String> listNarrowerChildConceptIds(String parentConceptId, String thesaurusId) {
        return entityManager.createNativeQuery("""
                        SELECT id_concept2
                        FROM hierarchical_relationship
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept1 = :parentConceptId
                          AND role LIKE 'NT%'
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("parentConceptId", parentConceptId)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<String> listBroaderParentConceptIds(String narrowerConceptId, String thesaurusId) {
        return entityManager.createNativeQuery("""
                        SELECT id_concept2
                        FROM hierarchical_relationship
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept1 = :narrowerConceptId
                          AND role LIKE 'BT%'
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("narrowerConceptId", narrowerConceptId)
                .getResultList();
    }

    @Transactional
    public void addCustomRelation(
            String conceptId1,
            String conceptId2,
            String thesaurusId,
            String relationType,
            boolean reciprocal,
            int userId
    ) {
        insertRelationship(conceptId1, conceptId2, thesaurusId, relationType);
        if (reciprocal) {
            insertRelationship(conceptId2, conceptId1, thesaurusId, relationType);
        }
        insertRelationHistory(conceptId1, conceptId2, thesaurusId, relationType, userId, "ADD");
    }

    @Transactional
    public void deleteCustomRelation(
            String conceptId1,
            String conceptId2,
            String thesaurusId,
            String relationCode,
            boolean reciprocal,
            int userId
    ) {
        deleteRelationship(conceptId1, conceptId2, thesaurusId, relationCode);
        if (reciprocal) {
            deleteRelationship(conceptId2, conceptId1, thesaurusId, relationCode);
        }
        insertRelationHistory(conceptId1, conceptId2, thesaurusId, "QUALIFIER", userId, "DEL");
    }

    private void insertRelationship(String fromId, String toId, String thesaurusId, String role) {
        entityManager.createNativeQuery("""
                        INSERT INTO hierarchical_relationship (
                            id_concept1, id_concept2, id_thesaurus, role
                        )
                        VALUES (:conceptId1, :conceptId2, :thesaurusId, :role)
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID1, fromId)
                .setParameter(NativeQueryParams.CONCEPT_ID2, toId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("role", role)
                .executeUpdate();
    }

    private void deleteHierarchicalPair(String conceptId1, String conceptId2, String thesaurusId) {
        entityManager.createNativeQuery("""
                        DELETE FROM hierarchical_relationship
                        WHERE id_thesaurus = :thesaurusId
                          AND (
                            (id_concept1 = :conceptId1 AND id_concept2 = :conceptId2)
                            OR (id_concept1 = :conceptId2 AND id_concept2 = :conceptId1)
                          )
                          AND (role LIKE 'BT%' OR role LIKE 'NT%')
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
                .executeUpdate();
    }

    private void deleteRelationship(String fromId, String toId, String thesaurusId, String role) {
        entityManager.createNativeQuery("""
                        DELETE FROM hierarchical_relationship
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept1 = :conceptId1
                          AND id_concept2 = :conceptId2
                          AND role = :role
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID1, fromId)
                .setParameter(NativeQueryParams.CONCEPT_ID2, toId)
                .setParameter("role", role)
                .executeUpdate();
    }

    private void insertRelationHistory(
            String conceptId1,
            String conceptId2,
            String thesaurusId,
            String role,
            int userId,
            String action
    ) {
        changeLog.record(ConceptChangeLog.Change.of(thesaurusId, conceptId1, ConceptChangeLog.RELATION, conceptId2, null, action,
                isDelete(action) ? conceptId2 : null, isDelete(action) ? null : conceptId2, role, userId));
    }

    private static boolean isDelete(String action) {
        return "DELETE".equals(ConceptChangeLog.normalizeActionPublic(action));
    }
}
