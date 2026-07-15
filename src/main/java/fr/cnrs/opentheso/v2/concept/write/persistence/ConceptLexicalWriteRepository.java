package fr.cnrs.opentheso.v2.concept.write.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConceptLexicalWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<String> findPreferredTermId(String thesaurusId, String conceptId) {
        return entityManager.createNativeQuery("""
                        SELECT id_term
                        FROM preferred_term
                        WHERE id_thesaurus = :thesaurusId
                          AND id_concept = :conceptId
                        LIMIT 1
                        """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getResultStream()
                .map(String.class::cast)
                .findFirst();
    }

    public boolean existsPrefLabel(String value, String lang, String thesaurusId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT COUNT(*) > 0
                        FROM term
                        WHERE f_unaccent(lower(lexical_value)) LIKE f_unaccent(lower(:value))
                          AND lang = :lang
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult());
    }

    public boolean existsAltLabel(String value, String lang, String thesaurusId) {
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

    public boolean existsTermIgnoreCase(String value, String lang, String thesaurusId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT COUNT(*) > 0
                        FROM term
                        WHERE lexical_value ILIKE :value
                          AND lang = :lang
                          AND id_thesaurus = :thesaurusId
                        """)
                .setParameter("value", value)
                .setParameter("lang", lang)
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult());
    }

    public Optional<String> findPreferredLabel(String conceptId, String thesaurusId, String lang) {
        return entityManager.createNativeQuery("""
                        SELECT t.lexical_value
                        FROM preferred_term pt
                        JOIN term t
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
                .map(String.class::cast)
                .findFirst();
    }
}
