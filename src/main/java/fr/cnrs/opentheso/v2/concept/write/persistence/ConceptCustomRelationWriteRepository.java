package fr.cnrs.opentheso.v2.concept.write.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConceptCustomRelationWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<String> findConceptTypeCode(String conceptId, String thesaurusId) {
        return entityManager.createNativeQuery("""
                        SELECT concept_type
                        FROM concept
                        WHERE id_concept = :conceptId
                          AND id_thesaurus = :thesaurusId
                        LIMIT 1
                        """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultStream()
                .map(String.class::cast)
                .findFirst();
    }

    public Optional<Boolean> findConceptTypeReciprocal(String conceptTypeCode, String thesaurusId) {
        return entityManager.createNativeQuery("""
                        SELECT reciprocal
                        FROM concept_type
                        WHERE code = :conceptTypeCode
                          AND id_theso IN (:thesaurusId, 'all')
                        ORDER BY CASE WHEN id_theso = :thesaurusId THEN 0 ELSE 1 END
                        LIMIT 1
                        """)
                .setParameter("conceptTypeCode", conceptTypeCode)
                .setParameter("thesaurusId", thesaurusId)
                .getResultStream()
                .map(value -> (Boolean) value)
                .findFirst();
    }
}
