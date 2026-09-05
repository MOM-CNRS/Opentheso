package fr.cnrs.opentheso.v2.concept.write.persistence;

import fr.cnrs.opentheso.v2.shared.repository.NativeQueryParams;
import fr.cnrs.opentheso.utils.ToolsHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class ConceptCreationWriteRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Optional<Integer> findIdentifierType(String thesaurusId) {
        return entityManager.createNativeQuery("""
                        SELECT identifier_type
                        FROM preferences
                        WHERE id_thesaurus = :thesaurusId
                        LIMIT 1
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultStream()
                .map(value -> ((Number) value).intValue())
                .findFirst();
    }

    public boolean existsConcept(String thesaurusId, String conceptId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM concept
                            WHERE id_thesaurus = :thesaurusId
                              AND id_concept = :conceptId
                        )
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .getSingleResult());
    }

    public boolean existsConceptGlobally(String conceptId) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1 FROM concept WHERE id_concept = :conceptId
                        )
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .getSingleResult());
    }

    public boolean existsNotation(String thesaurusId, String notation) {
        return Boolean.TRUE.equals(entityManager.createNativeQuery("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM concept
                            WHERE id_thesaurus = :thesaurusId
                              AND notation ILIKE :notation
                        )
                        """)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("notation", notation)
                .getSingleResult());
    }

    public Long nextNumericConceptId() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('concept__id_seq')")
                .getSingleResult()).longValue();
    }

    /**
     * Réserve {@code count} identifiants numériques en une seule requête (évite N× nextval).
     */
    public List<Long> reserveNumericConceptIds(int count) {
        if (count <= 0) {
            return List.of();
        }
        if (count == 1) {
            return List.of(nextNumericConceptId());
        }
        Number end = (Number) entityManager.createNativeQuery(
                        "SELECT setval('concept__id_seq', nextval('concept__id_seq') + :extra)")
                .setParameter("extra", count - 1)
                .getSingleResult();
        long endId = end.longValue();
        long startId = endId - count + 1;
        List<Long> ids = new ArrayList<>(count);
        for (long id = startId; id <= endId; id++) {
            ids.add(id);
        }
        return ids;
    }

    public String generateConceptId(String thesaurusId, String customConceptId) {
        if (customConceptId != null && !customConceptId.isBlank()) {
            return customConceptId.trim();
        }
        Integer identifierType = findIdentifierType(thesaurusId).orElse(2);
        if (identifierType == 1) {
            return generateAlphaNumericId();
        }
        return generateNumericConceptId();
    }

    @Transactional
    public void insertConcept(
            String conceptId,
            String thesaurusId,
            String status,
            String notation,
            boolean topConcept,
            int userId
    ) {
        Date now = new Date();
        entityManager.createNativeQuery("""
                        INSERT INTO concept (
                            id_concept, id_thesaurus, id_ark, created, modified, status,
                            notation, top_concept, creator, contributor, concept_type, gps,
                            id_handle, id_doi
                        )
                        VALUES (
                            :conceptId, :thesaurusId, '', :created, :modified, :status,
                            :notation, :topConcept, :userId, :userId, 'concept', false,
                            '', ''
                        )
                        """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("created", now)
                .setParameter(NativeQueryParams.MODIFIED, now)
                .setParameter("status", status)
                .setParameter("notation", notation == null ? "" : notation)
                .setParameter("topConcept", topConcept)
                .setParameter(NativeQueryParams.USER_ID, userId)
                .executeUpdate();
    }

    @Transactional
    public void linkConceptToGroup(String groupId, String conceptId, String thesaurusId) {
        entityManager.createNativeQuery("""
                        INSERT INTO concept_group_concept (idgroup, idthesaurus, idconcept)
                        VALUES (:groupId, :thesaurusId, :conceptId)
                        """)
                .setParameter(NativeQueryParams.GROUP_ID, groupId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .executeUpdate();
    }

    private String generateNumericConceptId() {
        long candidate = nextNumericConceptId();
        String conceptId = String.valueOf(candidate);
        while (existsConceptGlobally(conceptId)) {
            conceptId = String.valueOf(++candidate);
        }
        return conceptId;
    }

    private String generateAlphaNumericId() {
        String conceptId = ToolsHelper.getNewId(15, false, false);
        while (existsConceptGlobally(conceptId)) {
            conceptId = ToolsHelper.getNewId(15, false, false);
        }
        return conceptId;
    }
}
