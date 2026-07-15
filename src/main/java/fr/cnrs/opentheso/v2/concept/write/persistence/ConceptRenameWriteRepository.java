package fr.cnrs.opentheso.v2.concept.write.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Repository
public class ConceptRenameWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public record TermReference(int internalId, String idTerm, String lexicalValue) {
    }

    public Optional<TermReference> findTermByLexicalValue(String lexicalValue, String lang, String thesaurusId) {
        return entityManager.createNativeQuery("""
                        SELECT id, id_term, lexical_value
                        FROM term
                        WHERE lexical_value = :lexicalValue
                          AND lang = :lang
                          AND id_thesaurus = :thesaurusId
                        LIMIT 1
                        """)
                .setParameter("lexicalValue", lexicalValue)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getResultStream()
                .map(row -> {
                    Object[] values = (Object[]) row;
                    return new TermReference(
                            ((Number) values[0]).intValue(),
                            (String) values[1],
                            (String) values[2]
                    );
                })
                .findFirst();
    }

    public Optional<Integer> findTermInternalIdForConcept(String conceptId, String thesaurusId, String lang) {
        return entityManager.createNativeQuery("""
                        SELECT t.id
                        FROM term t
                        JOIN preferred_term pt
                          ON pt.id_term = t.id_term
                         AND pt.id_thesaurus = t.id_thesaurus
                        WHERE pt.id_concept = :conceptId
                          AND pt.id_thesaurus = :thesaurusId
                          AND t.lang = :lang
                        LIMIT 1
                        """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultStream()
                .map(value -> ((Number) value).intValue())
                .findFirst();
    }

    public boolean existsTermInLang(String idTerm, String thesaurusId, String lang) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM term
                            WHERE id_term = :idTerm
                              AND id_thesaurus = :thesaurusId
                              AND lang = :lang
                        )
                        """)
                .setParameter("idTerm", idTerm)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getSingleResult());
    }

    @Transactional
    public String createPreferredTermForConcept(
            String conceptId,
            String thesaurusId,
            String lang,
            String lexicalValue,
            String source,
            int userId
    ) {
        String idTerm = generateNextIdTerm(thesaurusId);
        Date now = new Date();
        entityManager.createNativeQuery("""
                        INSERT INTO term (
                            id_term, lexical_value, lang, id_thesaurus,
                            source, status, contributor, creator, created, modified
                        )
                        VALUES (
                            :idTerm, :lexicalValue, :lang, :thesaurusId,
                            :source, '', :userId, :userId, :created, :modified
                        )
                        """)
                .setParameter("idTerm", idTerm)
                .setParameter("lexicalValue", lexicalValue)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("source", source)
                .setParameter("userId", userId)
                .setParameter("created", now)
                .setParameter("modified", now)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        INSERT INTO term_historique (
                            id_term, lexical_value, lang, id_thesaurus,
                            id_user, action, modified, source, status
                        )
                        VALUES (
                            :idTerm, :lexicalValue, :lang, :thesaurusId,
                            :userId, 'ADD', :modified, :source, ''
                        )
                        """)
                .setParameter("idTerm", idTerm)
                .setParameter("lexicalValue", lexicalValue)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("userId", userId)
                .setParameter("modified", now)
                .setParameter("source", source)
                .executeUpdate();
        entityManager.createNativeQuery("""
                        INSERT INTO preferred_term (id_concept, id_term, id_thesaurus)
                        VALUES (:conceptId, :idTerm, :thesaurusId)
                        """)
                .setParameter("conceptId", conceptId)
                .setParameter("idTerm", idTerm)
                .setParameter("thesaurusId", thesaurusId)
                .executeUpdate();
        return idTerm;
    }

    private String generateNextIdTerm(String thesaurusId) {
        Number maxId = (Number) entityManager.createNativeQuery("SELECT COALESCE(MAX(id), 0) FROM term")
                .getSingleResult();
        int candidate = maxId.intValue();
        String idTerm;
        do {
            idTerm = String.valueOf(++candidate);
        } while (Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1 FROM term
                            WHERE id_term = :idTerm AND id_thesaurus = :thesaurusId
                        )
                        """)
                .setParameter("idTerm", idTerm)
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult()));
        return idTerm;
    }
}
