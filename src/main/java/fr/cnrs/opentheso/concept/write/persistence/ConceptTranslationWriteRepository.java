package fr.cnrs.opentheso.concept.write.persistence;

import fr.cnrs.opentheso.shared.repository.NativeQueryParams;
import fr.cnrs.opentheso.concept.history.ConceptChangeLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public class ConceptTranslationWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Journal unique des modifications (concept_change_log), qui remplace les anciennes tables *_historique. */
    @org.springframework.beans.factory.annotation.Autowired
    private ConceptChangeLog changeLog;

    @Transactional
    public void insertTranslation(
            String idTerm,
            String thesaurusId,
            String lang,
            String lexicalValue,
            int userId
    ) {
        entityManager.createNativeQuery("""
                        INSERT INTO term (
                            id_term, lexical_value, lang, id_thesaurus,
                            source, status, contributor, creator, created, modified
                        )
                        VALUES (
                            :idTerm, :lexicalValue, :lang, :thesaurusId,
                            '', '', :userId, :userId, :created, :modified
                        )
                        """)
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("created", Instant.now())
                .setParameter(NativeQueryParams.MODIFIED, Instant.now())
                .executeUpdate();
        insertHistory(idTerm, lexicalValue, thesaurusId, lang, userId, "New");
    }

    @Transactional
    public boolean updateTranslation(
            String idTerm,
            String thesaurusId,
            String lang,
            String lexicalValue,
            int userId
    ) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE term
                        SET lexical_value = :lexicalValue,
                            contributor = :userId,
                            modified = :modified
                        WHERE id_term = :idTerm
                          AND id_thesaurus = :thesaurusId
                          AND lang = :lang
                        """)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.MODIFIED, Instant.now())
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .executeUpdate();
        if (updated <= 0) {
            return false;
        }
        insertHistory(idTerm, lexicalValue, thesaurusId, lang, userId, "UPDATE");
        return true;
    }

    @Transactional
    public void deleteTranslation(String idTerm, String thesaurusId, String lang) {
        @SuppressWarnings("unchecked")
        java.util.List<String> previous = entityManager.createNativeQuery(
                        "SELECT lexical_value FROM term WHERE id_term = :idTerm AND lang = :lang AND id_thesaurus = :thesaurusId")
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
        if (!previous.isEmpty()) {
            changeLog.recordForTerm(idTerm, ConceptChangeLog.Change.of(thesaurusId, null, ConceptChangeLog.LABEL, idTerm, lang, "DELETE",
                isDelete("DELETE") ? previous.get(0) : null, isDelete("DELETE") ? null : previous.get(0), null, null));
        }
        entityManager.createNativeQuery("""
                        DELETE FROM term
                        WHERE id_term = :idTerm
                          AND lang = :lang
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
    }

    private void insertHistory(
            String idTerm,
            String lexicalValue,
            String thesaurusId,
            String lang,
            int userId,
            String action
    ) {
        changeLog.recordForTerm(idTerm, ConceptChangeLog.Change.of(thesaurusId, null, ConceptChangeLog.LABEL, idTerm, lang, action,
                isDelete(action) ? lexicalValue : null, isDelete(action) ? null : lexicalValue, null, userId));
    }

    private static boolean isDelete(String action) {
        return "DELETE".equals(ConceptChangeLog.normalizeActionPublic(action));
    }
}
