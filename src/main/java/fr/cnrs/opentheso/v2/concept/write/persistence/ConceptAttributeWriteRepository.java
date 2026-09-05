package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.shared.repository.NativeQueryParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
public class ConceptAttributeWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public boolean existsNotation(String thesaurusId, String notation, String excludeConceptId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM concept
                            WHERE id_thesaurus = :thesaurusId
                              AND notation ILIKE :notation
                              AND id_concept <> :excludeConceptId
                        )
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("notation", notation)
                .setParameter("excludeConceptId", excludeConceptId)
                .getSingleResult());
    }

    @Transactional
    public boolean updateNotation(String thesaurusId, String conceptId, String notation) {
        return entityManager.createNativeQuery("""
                        UPDATE concept
                        SET notation = :notation,
                            modified = :modified
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        """)
                .setParameter("notation", notation)
                .setParameter(NativeQueryParams.MODIFIED, new Date())
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .executeUpdate() > 0;
    }

    @Transactional
    public void updateConceptType(String thesaurusId, String conceptId, String conceptTypeCode) {
        entityManager.createNativeQuery("""
                        UPDATE concept
                        SET concept_type = :conceptType,
                            modified = :modified
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        """)
                .setParameter("conceptType", conceptTypeCode)
                .setParameter(NativeQueryParams.MODIFIED, new Date())
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .executeUpdate();
    }
}
