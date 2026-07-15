package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.concept.model.ConceptLinkItem;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusMetadataItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class ThesaurusHomeQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public int countValidConcepts(String thesaurusId) {
        String sql = """
                SELECT COUNT(id_concept)
                FROM concept
                WHERE id_thesaurus = :thesaurusId
                  AND status NOT IN ('CA', 'DEP', 'dep')
                """;
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .getSingleResult();
        return count != null ? count.intValue() : 0;
    }

    public Optional<Date> findLastModificationDate(String thesaurusId) {
        String sql = """
                SELECT c.modified
                FROM concept c
                WHERE c.id_thesaurus = :thesaurusId
                  AND c.status <> 'CA'
                  AND c.modified IS NOT NULL
                ORDER BY c.modified DESC
                LIMIT 1
                """;
        @SuppressWarnings("unchecked")
        List<Object> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return Optional.empty();
        }
        Date date = toDate(rows.get(0));
        return date != null ? Optional.of(date) : Optional.empty();
    }

    public Optional<String> findProjectName(String thesaurusId) {
        String sql = """
                SELECT ugl.label_group
                FROM user_group_thesaurus ugt
                JOIN user_group_label ugl ON ugt.id_group = ugl.id_group
                WHERE ugt.id_thesaurus = :thesaurusId
                LIMIT 1
                """;
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        if (rows.isEmpty() || StringUtils.isBlank(rows.get(0))) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public Optional<String> findArkId(String thesaurusId) {
        String sql = """
                SELECT COALESCE(id_ark, '')
                FROM thesaurus
                WHERE id_thesaurus = :thesaurusId
                """;
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        if (rows.isEmpty() || StringUtils.isBlank(rows.get(0))) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public String findHomePageHtml(String thesaurusId, String lang) {
        String sql = """
                SELECT htmlcode
                FROM thesohomepage
                WHERE idtheso = :thesaurusId
                  AND lang = :lang
                LIMIT 1
                """;
        @SuppressWarnings("unchecked")
        List<String> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty() || rows.get(0) == null) {
            return "";
        }
        return rows.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<ThesaurusMetadataItem> findMetadata(String thesaurusId) {
        String sql = """
                SELECT id, name, value, language, data_type
                FROM thesaurus_dcterms
                WHERE id_thesaurus = :thesaurusId
                ORDER BY name
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> new ThesaurusMetadataItem(
                        ((Number) row[0]).intValue(),
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? (String) row[2] : "",
                        row[3] != null ? (String) row[3] : "",
                        row[4] != null ? (String) row[4] : ""
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<ConceptLinkItem> findLastModifiedConcepts(String thesaurusId, String lang) {
        String sql = """
                SELECT c.id_concept, t.lexical_value
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.id_thesaurus = :thesaurusId
                  AND t.lang = :lang
                  AND c.status != 'CA'
                  AND c.modified IS NOT NULL
                ORDER BY c.modified DESC, t.lexical_value
                LIMIT 10
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream()
                .map(row -> new ConceptLinkItem(
                        row[0] != null ? (String) row[0] : "",
                        row[1] != null ? (String) row[1] : ""
                ))
                .filter(item -> StringUtils.isNotBlank(item.label()))
                .toList();
    }

    private Date toDate(Object value) {
        if (value instanceof Date date) {
            return date;
        }
        if (value instanceof Timestamp timestamp) {
            return new Date(timestamp.getTime());
        }
        if (value instanceof Instant instant) {
            return Date.from(instant);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        return null;
    }
}
