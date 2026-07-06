package fr.cnrs.opentheso.v2.concept.write.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Repository
public class ConceptSynonymWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

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
                .setParameter("idTerm", idTerm)
                .setParameter("lexicalValue", lexicalValue)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("status", hidden ? "Hidden" : "USE")
                .setParameter("hidden", hidden)
                .setParameter("created", new Date())
                .setParameter("modified", new Date())
                .executeUpdate();
        insertHistory(idTerm, lexicalValue, thesaurusId, lang, userId, hidden, "ADD");
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
                .setParameter("hidden", hidden)
                .setParameter("modified", new Date())
                .setParameter("idTerm", idTerm)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("oldValue", oldValue)
                .executeUpdate();
        if (updated <= 0) {
            return false;
        }
        insertHistory(idTerm, newValue, thesaurusId, lang, userId, hidden, "update");
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
                .setParameter("hidden", hidden)
                .setParameter("modified", new Date())
                .setParameter("idTerm", idTerm)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("value", value)
                .executeUpdate();
        if (updated <= 0) {
            return false;
        }
        insertHistory(idTerm, value, thesaurusId, lang, userId, hidden, "update");
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
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("idTerm", idTerm)
                .setParameter("lexicalValue", lexicalValue)
                .setParameter("lang", lang)
                .executeUpdate();
        insertHistory(idTerm, lexicalValue, thesaurusId, lang, userId, false, "delete");
    }

    private boolean existsAltLabel(String value, String lang, String thesaurusId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT COUNT(*) > 0
                        FROM non_preferred_term
                        WHERE f_unaccent(lower(lexical_value)) = f_unaccent(lower(:value))
                          AND id_thesaurus = :thesaurusId
                          AND lang = :lang
                        """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult());
    }

    private void insertHistory(
            String idTerm,
            String lexicalValue,
            String thesaurusId,
            String lang,
            int userId,
            boolean hidden,
            String action
    ) {
        entityManager.createNativeQuery("""
                        INSERT INTO non_preferred_term_historique (
                            id_term, lexical_value, lang, id_thesaurus,
                            id_user, hiden, action, modified, source, status
                        )
                        VALUES (
                            :idTerm, :lexicalValue, :lang, :thesaurusId,
                            :userId, :hidden, :action, :modified, '', ''
                        )
                        """)
                .setParameter("idTerm", idTerm)
                .setParameter("lexicalValue", lexicalValue)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("userId", userId)
                .setParameter("hidden", hidden)
                .setParameter("action", action)
                .setParameter("modified", new Date())
                .executeUpdate();
    }
}
