package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.shared.repository.NativeQueryParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Repository
public class ConceptRelationWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

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
        deleteRelationship(thesaurusId, narrowerConceptId, broaderConceptId, "BT");
        deleteRelationship(thesaurusId, broaderConceptId, narrowerConceptId, "NT");
    }

    @Transactional
    public void deleteNarrowerRelation(String broaderConceptId, String narrowerConceptId, String thesaurusId, int userId) {
        insertRelationHistory(broaderConceptId, narrowerConceptId, thesaurusId, "RT", userId, "DELETE");
        deleteRelationship(thesaurusId, broaderConceptId, narrowerConceptId, "NT");
        deleteRelationship(thesaurusId, narrowerConceptId, broaderConceptId, "BT");
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
        deleteRelationship(thesaurusId, conceptId1, conceptId2, "RT");
        deleteRelationship(thesaurusId, conceptId2, conceptId1, "RT");
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
        deleteRelationship(thesaurusId, conceptId1, conceptId2, relationCode);
        if (reciprocal) {
            deleteRelationship(thesaurusId, conceptId2, conceptId1, relationCode);
        }
        insertRelationHistory(conceptId1, conceptId2, thesaurusId, "QUALIFIER", userId, "DEL");
    }

    private void insertRelationship(String conceptId1, String conceptId2, String thesaurusId, String role) {
        entityManager.createNativeQuery("""
                        INSERT INTO hierarchical_relationship (
                            id_concept1, id_concept2, id_thesaurus, role
                        )
                        VALUES (:conceptId1, :conceptId2, :thesaurusId, :role)
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("role", role)
                .executeUpdate();
    }

    private void deleteRelationship(String thesaurusId, String conceptId1, String conceptId2, String role) {
        entityManager.createNativeQuery("""
                        DELETE FROM hierarchical_relationship
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept1 = :conceptId1
                          AND id_concept2 = :conceptId2
                          AND role = :role
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
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
        entityManager.createNativeQuery("""
                        INSERT INTO hierarchical_relationship_historique (
                            id_concept1, id_concept2, id_thesaurus, role,
                            modified, id_user, action
                        )
                        VALUES (
                            :conceptId1, :conceptId2, :thesaurusId, :role,
                            :modified, :userId, :action
                        )
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID1, conceptId1)
                .setParameter(NativeQueryParams.CONCEPT_ID2, conceptId2)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("role", role)
                .setParameter(NativeQueryParams.MODIFIED, new Date())
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("action", action)
                .executeUpdate();
    }
}
