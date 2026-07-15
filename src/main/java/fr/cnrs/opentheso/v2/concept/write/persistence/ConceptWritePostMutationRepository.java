package fr.cnrs.opentheso.v2.concept.write.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ConceptWritePostMutationRepository {

    private static final String CONTRIBUTOR_DC_TERM = "contributor";
    private static final String CREATOR_DC_TERM = "creator";

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void touchConcept(String thesaurusId, String conceptId, int contributorUserId) {
        entityManager.createNativeQuery("""
                        UPDATE concept
                        SET modified = CURRENT_DATE,
                            contributor = :contributor
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        """)
                .setParameter("contributor", contributorUserId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .executeUpdate();
    }

    @Transactional
    public void saveContributorDcTerm(String thesaurusId, String conceptId, String contributorName) {
        entityManager.createNativeQuery("""
                        INSERT INTO concept_dcterms (id_concept, id_thesaurus, name, value)
                        VALUES (:conceptId, :thesaurusId, :name, :value)
                        """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("name", CONTRIBUTOR_DC_TERM)
                .setParameter("value", contributorName)
                .executeUpdate();
    }

    @Transactional
    public void saveCreatorDcTerm(String thesaurusId, String conceptId, String creatorName) {
        entityManager.createNativeQuery("""
                        INSERT INTO concept_dcterms (id_concept, id_thesaurus, name, value)
                        VALUES (:conceptId, :thesaurusId, :name, :value)
                        """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("name", CREATOR_DC_TERM)
                .setParameter("value", creatorName)
                .executeUpdate();
    }
}
