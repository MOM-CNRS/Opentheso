package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional(readOnly = true)
public class HistoryQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Object[]> findTermHistories(String termId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT th.lexical_value, th.lang, th.action, th.modified, u.username
            FROM term_historique th
            JOIN users u ON u.id_user = th.id_user
            WHERE th.id_term = :termId
              AND th.id_thesaurus = :thesaurusId
            ORDER BY th.modified DESC
            """)
                .setParameter(NativeQueryParams.TERM_ID, termId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
    }

    public List<Object[]> findSynonymHistories(String termId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT npth.lexical_value, npth.lang, npth.action, npth.modified, u.username
            FROM non_preferred_term_historique npth
            JOIN users u ON u.id_user = npth.id_user
            WHERE npth.id_term = :termId
              AND npth.id_thesaurus = :thesaurusId
            ORDER BY npth.modified DESC
            """)
                .setParameter(NativeQueryParams.TERM_ID, termId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
    }

    public List<Object[]> findRelationHistories(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT hr.id_concept2, hr.role, hr.action, hr.modified, u.username
            FROM hierarchical_relationship_historique hr
            JOIN users u ON u.id_user = hr.id_user
            WHERE hr.id_concept1 = :conceptId
              AND hr.id_thesaurus = :thesaurusId
            ORDER BY hr.modified DESC
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
    }

    public List<Object[]> findNoteHistories(String conceptId, String termId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT nh.lexicalvalue, nh.notetypecode, nh.lang, nh.action_performed, nh.modified, u.username
            FROM note_historique nh
            JOIN users u ON u.id_user = nh.id_user
            WHERE (nh.id_concept = :conceptId OR nh.id_term = :termId)
              AND nh.id_thesaurus = :thesaurusId
            ORDER BY nh.modified DESC
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.TERM_ID, termId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
    }
}
