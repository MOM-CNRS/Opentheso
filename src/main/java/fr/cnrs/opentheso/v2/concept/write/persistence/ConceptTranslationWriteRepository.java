package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.shared.repository.NativeQueryParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
public class ConceptTranslationWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

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
                .setParameter("created", new Date())
                .setParameter(NativeQueryParams.MODIFIED, new Date())
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
                .setParameter(NativeQueryParams.MODIFIED, new Date())
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
        entityManager.createNativeQuery("""
                        INSERT INTO term_historique (
                            id_term, lexical_value, lang, id_thesaurus,
                            id_user, action, modified, source, status
                        )
                        VALUES (
                            :idTerm, :lexicalValue, :lang, :thesaurusId,
                            :userId, :action, :modified, '', ''
                        )
                        """)
                .setParameter(NativeQueryParams.ID_TERM, idTerm)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("action", action)
                .setParameter(NativeQueryParams.MODIFIED, new Date())
                .executeUpdate();
    }
}
