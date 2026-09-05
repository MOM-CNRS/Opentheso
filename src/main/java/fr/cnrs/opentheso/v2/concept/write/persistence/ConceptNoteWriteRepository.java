package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.shared.repository.NativeQueryParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Repository
public class ConceptNoteWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<String> findNoteLexicalValue(
            String conceptId,
            String thesaurusId,
            String lang,
            String typeCode
    ) {
        return entityManager.createNativeQuery("""
                        SELECT lexicalvalue
                        FROM note
                        WHERE identifier = :conceptId
                          AND id_thesaurus = :thesaurusId
                          AND notetypecode = :typeCode
                          AND lang = :lang
                        LIMIT 1
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.TYPE_CODE, typeCode)
                .setParameter("lang", lang)
                .getResultStream()
                .map(String.class::cast)
                .findFirst();
    }

    public Optional<Integer> findNoteId(
            String conceptId,
            String thesaurusId,
            String lang,
            String typeCode
    ) {
        Number id = (Number) entityManager.createNativeQuery("""
                        SELECT id
                        FROM note
                        WHERE identifier = :conceptId
                          AND id_thesaurus = :thesaurusId
                          AND notetypecode = :typeCode
                          AND lang = :lang
                        LIMIT 1
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.TYPE_CODE, typeCode)
                .setParameter("lang", lang)
                .getResultStream()
                .findFirst()
                .orElse(null);
        return id == null ? Optional.empty() : Optional.of(id.intValue());
    }

    public Optional<String> findNoteSource(
            String conceptId,
            String thesaurusId,
            String lang,
            String typeCode
    ) {
        return entityManager.createNativeQuery("""
                        SELECT notesource
                        FROM note
                        WHERE identifier = :conceptId
                          AND id_thesaurus = :thesaurusId
                          AND notetypecode = :typeCode
                          AND lang = :lang
                        LIMIT 1
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.TYPE_CODE, typeCode)
                .setParameter("lang", lang)
                .getResultStream()
                .map(value -> value != null ? (String) value : "")
                .findFirst();
    }

    public boolean existsWithValue(
            String conceptId,
            String thesaurusId,
            String lang,
            String typeCode,
            String lexicalValue
    ) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM note
                            WHERE identifier = :conceptId
                              AND id_thesaurus = :thesaurusId
                              AND notetypecode = :typeCode
                              AND lang = :lang
                              AND lexicalvalue = :lexicalValue
                        )
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.TYPE_CODE, typeCode)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .getSingleResult());
    }

    @Transactional
    public void insertNote(
            String conceptId,
            String thesaurusId,
            String lang,
            String typeCode,
            String lexicalValue,
            String noteSource,
            int userId
    ) {
        Date now = new Date();
        entityManager.createNativeQuery("""
                        INSERT INTO note (
                            notetypecode, id_thesaurus, lang, lexicalvalue,
                            identifier, notesource, id_user, created, modified
                        )
                        VALUES (
                            :typeCode, :thesaurusId, :lang, :lexicalValue,
                            :conceptId, :noteSource, :userId, :created, :modified
                        )
                        """)
                .setParameter(NativeQueryParams.TYPE_CODE, typeCode)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter("noteSource", noteSource)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter("created", now)
                .setParameter(NativeQueryParams.MODIFIED, now)
                .executeUpdate();
    }

    @Transactional
    public boolean updateNote(
            int noteId,
            String thesaurusId,
            String lexicalValue,
            String noteSource
    ) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE note
                        SET lexicalvalue = :lexicalValue,
                            notesource = :noteSource,
                            modified = :modified
                        WHERE id = :noteId
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter("noteSource", noteSource)
                .setParameter(NativeQueryParams.MODIFIED, new Date())
                .setParameter("noteId", noteId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
        return updated > 0;
    }

    @Transactional
    public void deleteNote(int noteId, String thesaurusId) {
        entityManager.createNativeQuery("""
                        DELETE FROM note
                        WHERE id = :noteId
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("noteId", noteId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .executeUpdate();
    }

    @Transactional
    public void insertNoteHistory(
            String conceptId,
            String thesaurusId,
            String lang,
            String typeCode,
            String lexicalValue,
            String action,
            int userId
    ) {
        entityManager.createNativeQuery("""
                        INSERT INTO note_historique (
                            notetypecode, id_thesaurus, id_concept, lang,
                            lexicalvalue, action_performed, id_user, modified
                        )
                        VALUES (
                            :typeCode, :thesaurusId, :conceptId, :lang,
                            :lexicalValue, :action, :userId, :modified
                        )
                        """)
                .setParameter(NativeQueryParams.TYPE_CODE, typeCode)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.LEXICAL_VALUE, lexicalValue)
                .setParameter("action", action)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .setParameter(NativeQueryParams.MODIFIED, new Date())
                .executeUpdate();
    }
}
