package fr.cnrs.opentheso.concept.write.persistence;

import fr.cnrs.opentheso.shared.repository.NativeQueryParams;
import fr.cnrs.opentheso.concept.history.ConceptChangeLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public class ConceptSynonymWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /** Journal unique des modifications (concept_change_log), qui remplace les anciennes tables *_historique. */
    @org.springframework.beans.factory.annotation.Autowired
    private ConceptChangeLog changeLog;

    @Transactional
    public boolean insertSynonym(
            String idTerm,
            String thesaurusId,
            String lang,
            String lexicalValue,
            boolean hidden,
            int userId
    ) {
        if (existsAltLabel(lexicalValue, lang, thesaurusId)) {
            return false;
        }
        entityManager.createNativeQuery("""
                        INSERT INTO non_preferred_term (
                            id_term, lexical_value, lang, id_thesaurus,
                            source, status, hiden, created, modified
                        )
                        VALUES (
                            :idTerm, :lexicalValue, :lang, :thesaurusId,
                            '', :status, :hidden, :created, :modified
                        )
                        """)
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("status", hidden ? "Hidden" : "USE")
                .setParameter(NativeQueryParams.HIDDEN, hidden)
                .setParameter("created", Instant.now())
                .setParameter(NativeQueryParams.MODIFIED, Instant.now())
                .executeUpdate();
        insertHistory(idTerm, lexicalValue, thesaurusId, lang, userId, hidden, "ADD", null);
        return true;
    }

    @Transactional
    public boolean updateSynonym(
            String idTerm,
            String thesaurusId,
            String lang,
            String oldValue,
            String newValue,
            boolean hidden,
            int userId
    ) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE non_preferred_term
                        SET lexical_value = :newValue,
                            hiden = :hidden,
                            modified = :modified
                        WHERE id_term = :idTerm
                          AND id_thesaurus = :thesaurusId
                          AND lang = :lang
                          AND lexical_value = :oldValue
                        """)
                .setParameter("newValue", newValue)
                .setParameter(NativeQueryParams.HIDDEN, hidden)
                .setParameter(NativeQueryParams.MODIFIED, Instant.now())
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter("oldValue", oldValue)
                .executeUpdate();
        if (updated <= 0) {
            return false;
        }
        insertHistory(idTerm, newValue, thesaurusId, lang, userId, hidden, "update", oldValue);
        return true;
    }

    @Transactional
    public boolean updateSynonymHidden(
            String idTerm,
            String thesaurusId,
            String lang,
            String value,
            boolean hidden,
            int userId
    ) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE non_preferred_term
                        SET hiden = :hidden,
                            modified = :modified
                        WHERE id_term = :idTerm
                          AND id_thesaurus = :thesaurusId
                          AND lang = :lang
                          AND lexical_value = :value
                        """)
                .setParameter(NativeQueryParams.HIDDEN, hidden)
                .setParameter(NativeQueryParams.MODIFIED, Instant.now())
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.VALUE, value)
                .executeUpdate();
        if (updated <= 0) {
            return false;
        }
        insertHistory(idTerm, value, thesaurusId, lang, userId, hidden, "update", value);
        return true;
    }

    @Transactional
    public void deleteSynonym(
            String idTerm,
            String thesaurusId,
            String lang,
            String lexicalValue,
            int userId
    ) {
        entityManager.createNativeQuery("""
                        DELETE FROM non_preferred_term
                        WHERE id_thesaurus = :thesaurusId
                          AND id_term = :idTerm
                          AND lexical_value = :lexicalValue
                          AND lang = :lang
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter("lang", lang)
                .executeUpdate();
        insertHistory(idTerm, lexicalValue, thesaurusId, lang, userId, false, "delete", null);
    }

    private boolean existsAltLabel(String value, String lang, String thesaurusId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT COUNT(*) > 0
                        FROM non_preferred_term
                        WHERE f_unaccent(lower(lexical_value)) = f_unaccent(lower(:value))
                          AND id_thesaurus = :thesaurusId
                          AND lang = :lang
                        """)
                .setParameter(NativeQueryParams.VALUE, value)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getSingleResult());
    }

    private void insertHistory(
            String idTerm,
            String lexicalValue,
            String thesaurusId,
            String lang,
            int userId,
            boolean hidden,
            String action,
            String oldValue
    ) {
        changeLog.recordForTerm(idTerm, ConceptChangeLog.Change.of(thesaurusId, null, ConceptChangeLog.SYNONYM, idTerm, lang, action,
                isDelete(action) ? lexicalValue : oldValue, isDelete(action) ? null : lexicalValue, hidden ? "hidden" : null, userId));
    }

    private static boolean isDelete(String action) {
        return "DELETE".equals(ConceptChangeLog.normalizeActionPublic(action));
    }
}
