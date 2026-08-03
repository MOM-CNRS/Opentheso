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
                        SET modified = CURRENT_TIMESTAMP,
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
        insertDcTermIfAbsent(thesaurusId, conceptId, CONTRIBUTOR_DC_TERM, contributorName);
    }

    @Transactional
    public void saveCreatorDcTerm(String thesaurusId, String conceptId, String creatorName) {
        insertDcTermIfAbsent(thesaurusId, conceptId, CREATOR_DC_TERM, creatorName);
    }

    /**
     * La PK de {@code concept_dcterms} est (id_concept, id_thesaurus, name, value).
     * Un même contributeur peut déjà être enregistré : on ignore alors le doublon.
     */
    private void insertDcTermIfAbsent(String thesaurusId, String conceptId, String name, String value) {
        entityManager.createNativeQuery("""
                        INSERT INTO concept_dcterms (id_concept, id_thesaurus, name, value)
                        VALUES (:conceptId, :thesaurusId, :name, :value)
                        ON CONFLICT (id_concept, id_thesaurus, name, value) DO NOTHING
                        """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("name", name)
                .setParameter("value", value)
                .executeUpdate();
    }
}
