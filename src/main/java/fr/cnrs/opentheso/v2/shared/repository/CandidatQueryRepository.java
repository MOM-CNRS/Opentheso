package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.candidat.mapper.CandidatDetailJsonParser;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatConceptRelationRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatDetailBundle;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatDiscussionRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatIdValueRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatListRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatNoteDetailRow;
import fr.cnrs.opentheso.v2.shared.repository.projection.CandidatTranslationRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class CandidatQueryRepository {

    private static final String UNKNOWN_USER = "Utilisateur inconnu";
    public static final int BOARD_LIST_CAP = 400;

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<CandidatListRow> findCandidatesByStatus(String thesaurusId, String lang, int statusId) {
        return findCandidatesByStatus(thesaurusId, lang, statusId, null);
    }

    @SuppressWarnings("unchecked")
    public List<CandidatListRow> findCandidatesByStatus(String thesaurusId, String lang, int statusId, String searchTerm) {
        String language = StringUtils.isBlank(lang) ? "fr" : lang;
        int searchFlag = StringUtils.isNotBlank(searchTerm) ? 1 : 0;
        String sql = """
                SELECT
                    cs.id_concept,
                    c.created,
                    c.modified,
                    cs.id_user,
                    cs.id_user_admin,
                    cs.message,
                    COALESCE(pref.lexical_value, '') AS preferred_label,
                    COALESCE(u_creator.username, :unknownUser) AS created_by,
                    COALESCE(u_admin.username, :unknownUser) AS created_by_admin,
                    COALESCE(msg.message_count, 0) AS message_count,
                    COALESCE(prop.proposition_count, 0) AS proposition_count,
                    COALESCE(v_ca.candidate_vote_count, 0) AS candidate_vote_count,
                    COALESCE(v_nt.note_vote_count, 0) AS note_vote_count
                FROM candidat_status cs
                JOIN concept c
                    ON cs.id_concept = c.id_concept
                    AND cs.id_thesaurus = c.id_thesaurus
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = c.id_concept
                    AND pt.id_thesaurus = c.id_thesaurus
                LEFT JOIN term pref
                    ON pref.id_term = pt.id_term
                    AND pref.id_thesaurus = pt.id_thesaurus
                    AND pref.lang = :lang
                LEFT JOIN users u_creator ON u_creator.id_user = cs.id_user
                LEFT JOIN users u_admin ON u_admin.id_user = cs.id_user_admin
                LEFT JOIN LATERAL (
                    SELECT COUNT(*)::int AS message_count
                    FROM candidat_messages cm
                    WHERE cm.id_concept = cs.id_concept
                      AND cm.id_thesaurus = cs.id_thesaurus
                ) msg ON true
                LEFT JOIN LATERAL (
                    SELECT COUNT(*)::int AS proposition_count
                    FROM proposition p
                    WHERE p.id_concept = cs.id_concept
                      AND p.id_thesaurus = cs.id_thesaurus
                ) prop ON true
                LEFT JOIN LATERAL (
                    SELECT COUNT(*)::int AS candidate_vote_count
                    FROM candidat_vote cv
                    WHERE cv.id_concept = cs.id_concept
                      AND cv.id_thesaurus = cs.id_thesaurus
                      AND cv.type_vote = 'CA'
                ) v_ca ON true
                LEFT JOIN LATERAL (
                    SELECT COUNT(*)::int AS note_vote_count
                    FROM candidat_vote cv
                    WHERE cv.id_concept = cs.id_concept
                      AND cv.id_thesaurus = cs.id_thesaurus
                      AND cv.type_vote = 'NT'
                ) v_nt ON true
                WHERE cs.id_thesaurus = :thesaurusId
                  AND cs.id_status = :statusId
                  AND (
                      :searchFlag = 0
                      OR f_unaccent(lower(COALESCE(pref.lexical_value, '')))
                          LIKE CONCAT('%', f_unaccent(lower(:searchTerm)), '%')
                      OR f_unaccent(lower(COALESCE(u_creator.username, '')))
                          LIKE CONCAT('%', f_unaccent(lower(:searchTerm)), '%')
                  )
                ORDER BY c.created DESC
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("statusId", statusId)
                .setParameter("lang", language)
                .setParameter("unknownUser", UNKNOWN_USER)
                .setParameter("searchFlag", searchFlag)
                .setParameter("searchTerm", searchFlag == 1 ? searchTerm.trim() : "")
                .setMaxResults(BOARD_LIST_CAP)
                .getResultList();
        return rows.stream().map(this::toCandidatListRow).toList();
    }

    public int countCandidatesByStatus(String thesaurusId, int statusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return 0;
        }
        Number result = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM candidat_status cs
                WHERE cs.id_thesaurus = :thesaurusId
                  AND cs.id_status = :statusId
                """)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("statusId", statusId)
                .getSingleResult();
        return result != null ? result.intValue() : 0;
    }

    public Optional<CandidatDetailBundle> findCandidateDetailBundle(
            String thesaurusId,
            String conceptId,
            String lang,
            int userId
    ) {
        String language = StringUtils.isBlank(lang) ? "fr" : lang;
        String sql = """
                SELECT
                    (SELECT pt.id_term
                     FROM preferred_term pt
                     WHERE pt.id_thesaurus = :thesaurusId
                       AND pt.id_concept = :conceptId
                     LIMIT 1) AS preferred_term_id,
                    EXISTS (
                        SELECT 1
                        FROM candidat_vote cv
                        WHERE cv.id_concept = :conceptId
                          AND cv.id_thesaurus = :thesaurusId
                          AND cv.id_user = :userId
                          AND cv.type_vote = 'CA'
                    ) AS voted,
                    (SELECT COALESCE(json_agg(json_build_object(
                            'id', cgc.idgroup,
                            'value', COALESCE(cgl.lexicalvalue, cgc.idgroup)
                        ) ORDER BY COALESCE(cgl.lexicalvalue, cgc.idgroup)), '[]'::json)
                     FROM concept_group_concept cgc
                     LEFT JOIN concept_group_label cgl
                         ON cgl.idgroup = cgc.idgroup
                         AND cgl.idthesaurus = cgc.idthesaurus
                         AND cgl.lang = :lang
                     WHERE cgc.idconcept = :conceptId
                       AND cgc.idthesaurus = :thesaurusId) AS collections_json,
                    (SELECT COALESCE(json_agg(json_build_object(
                            'id', hr.id_concept2,
                            'value', COALESCE(pref.lexical_value, '')
                        ) ORDER BY COALESCE(pref.lexical_value, '')), '[]'::json)
                     FROM hierarchical_relationship hr
                     LEFT JOIN preferred_term pt
                         ON pt.id_concept = hr.id_concept2
                         AND pt.id_thesaurus = hr.id_thesaurus
                     LEFT JOIN term pref
                         ON pref.id_term = pt.id_term
                         AND pref.id_thesaurus = pt.id_thesaurus
                         AND pref.lang = :lang
                     WHERE hr.id_thesaurus = :thesaurusId
                       AND hr.id_concept1 = :conceptId
                       AND hr.role LIKE 'BT%') AS broader_json,
                    (SELECT COALESCE(json_agg(json_build_object(
                            'id', hr.id_concept2,
                            'value', COALESCE(pref.lexical_value, '')
                        ) ORDER BY COALESCE(pref.lexical_value, '')), '[]'::json)
                     FROM hierarchical_relationship hr
                     JOIN concept c
                         ON hr.id_concept2 = c.id_concept
                         AND hr.id_thesaurus = c.id_thesaurus
                     LEFT JOIN preferred_term pt
                         ON pt.id_concept = hr.id_concept2
                         AND pt.id_thesaurus = hr.id_thesaurus
                     LEFT JOIN term pref
                         ON pref.id_term = pt.id_term
                         AND pref.id_thesaurus = pt.id_thesaurus
                         AND pref.lang = :lang
                     WHERE hr.id_thesaurus = :thesaurusId
                       AND hr.id_concept1 = :conceptId
                       AND hr.role = 'RT'
                       AND c.status != 'CA') AS related_json,
                    (SELECT COALESCE(json_agg(npt.lexical_value ORDER BY npt.lexical_value), '[]'::json)
                     FROM non_preferred_term npt
                     JOIN preferred_term pt
                         ON pt.id_term = npt.id_term
                         AND pt.id_thesaurus = npt.id_thesaurus
                     WHERE pt.id_concept = :conceptId
                       AND npt.id_thesaurus = :thesaurusId
                       AND npt.lang = :lang) AS synonyms_json,
                    (SELECT COALESCE(json_agg(json_build_object(
                            'id', n.id,
                            'noteTypeCode', n.notetypecode,
                            'idConcept', n.id_concept,
                            'lang', n.lang,
                            'lexicalValue', n.lexicalvalue,
                            'idUser', n.id_user
                        ) ORDER BY n.id), '[]'::json)
                     FROM note n
                     WHERE n.identifier = :conceptId
                       AND n.id_thesaurus = :thesaurusId) AS notes_json,
                    (SELECT COALESCE(json_agg(cv.id_note), '[]'::json)
                     FROM candidat_vote cv
                     WHERE cv.id_concept = :conceptId
                       AND cv.id_thesaurus = :thesaurusId
                       AND cv.id_user = :userId
                       AND cv.type_vote = 'NT') AS note_vote_ids_json,
                    (SELECT COALESCE(json_agg(json_build_object(
                            'lang', t.lang,
                            'lexicalValue', t.lexical_value,
                            'countryCode', COALESCE(l.code_pays, '')
                        ) ORDER BY t.lexical_value), '[]'::json)
                     FROM term t
                     JOIN preferred_term pt
                         ON pt.id_term = t.id_term
                         AND pt.id_thesaurus = t.id_thesaurus
                     JOIN languages_iso639 l ON t.lang = l.iso639_1
                     WHERE pt.id_concept = :conceptId
                       AND t.id_thesaurus = :thesaurusId
                       AND t.lang <> :lang) AS translations_json,
                    (SELECT COALESCE(json_agg(json_build_object(
                            'idUser', u.id_user,
                            'username', u.username,
                            'value', c.value,
                            'date', c.date
                        ) ORDER BY c.date), '[]'::json)
                     FROM candidat_messages c
                     JOIN users u ON c.id_user = u.id_user
                     WHERE c.id_concept = :conceptId
                       AND c.id_thesaurus = :thesaurusId) AS messages_json
                FROM concept anchor
                WHERE anchor.id_concept = :conceptId
                  AND anchor.id_thesaurus = :thesaurusId
                """;
        try {
            Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                    .setParameter("thesaurusId", thesaurusId)
                    .setParameter("conceptId", conceptId)
                    .setParameter("lang", language)
                    .setParameter("userId", userId)
                    .getSingleResult();
            var parsed = CandidatDetailJsonParser.parse(
                    stringValue(row[2]),
                    stringValue(row[3]),
                    stringValue(row[4]),
                    stringValue(row[5]),
                    stringValue(row[6]),
                    stringValue(row[7]),
                    stringValue(row[8]),
                    stringValue(row[9])
            );
            return Optional.of(new CandidatDetailBundle(
                    row[0] != null ? (String) row[0] : null,
                    toBoolean(row[1]),
                    parsed
            ));
        } catch (NoResultException ex) {
            return Optional.empty();
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "[]";
        }
        return value.toString();
    }

    private static boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public List<CandidatIdValueRow> findCollections(String thesaurusId, String conceptId, String lang) {
        String language = StringUtils.isBlank(lang) ? "fr" : lang;
        String sql = """
                SELECT cgc.idgroup, COALESCE(cgl.lexicalvalue, cgc.idgroup) AS label
                FROM concept_group_concept cgc
                LEFT JOIN concept_group_label cgl
                    ON cgl.idgroup = cgc.idgroup
                    AND cgl.idthesaurus = cgc.idthesaurus
                    AND cgl.lang = :lang
                WHERE cgc.idconcept = :conceptId
                  AND cgc.idthesaurus = :thesaurusId
                ORDER BY label
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .setParameter("lang", language)
                .getResultList();
        return rows.stream()
                .map(row -> new CandidatIdValueRow((String) row[0], row[1] != null ? (String) row[1] : ""))
                .toList();
    }

    public List<CandidatIdValueRow> findBroaderRelations(String thesaurusId, String conceptId, String lang) {
        return findRelations(thesaurusId, conceptId, lang, true);
    }

    public List<CandidatIdValueRow> findRelatedRelations(String thesaurusId, String conceptId, String lang) {
        return findRelations(thesaurusId, conceptId, lang, false);
    }

    @SuppressWarnings("unchecked")
    public List<CandidatConceptRelationRow> findBroaderRelationsForConcepts(String thesaurusId, List<String> conceptIds, String lang) {
        if (CollectionUtils.isEmpty(conceptIds)) {
            return Collections.emptyList();
        }
        String language = StringUtils.isBlank(lang) ? "fr" : lang;
        String sql = """
                SELECT hr.id_concept1, hr.id_concept2, COALESCE(pref.lexical_value, '') AS label
                FROM hierarchical_relationship hr
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = hr.id_concept2
                    AND pt.id_thesaurus = hr.id_thesaurus
                LEFT JOIN term pref
                    ON pref.id_term = pt.id_term
                    AND pref.id_thesaurus = pt.id_thesaurus
                    AND pref.lang = :lang
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 IN (:conceptIds)
                  AND hr.role LIKE 'BT%'
                ORDER BY hr.id_concept1, label
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptIds", conceptIds)
                .setParameter("lang", language)
                .getResultList();
        return rows.stream()
                .map(row -> new CandidatConceptRelationRow(
                        (String) row[0],
                        (String) row[1],
                        row[2] != null ? (String) row[2] : ""
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<String> findSynonyms(String thesaurusId, String conceptId, String lang) {
        String language = StringUtils.isBlank(lang) ? "fr" : lang;
        String sql = """
                SELECT npt.lexical_value
                FROM non_preferred_term npt
                JOIN preferred_term pt
                    ON pt.id_term = npt.id_term
                    AND pt.id_thesaurus = npt.id_thesaurus
                WHERE pt.id_concept = :conceptId
                  AND npt.id_thesaurus = :thesaurusId
                  AND npt.lang = :lang
                ORDER BY npt.lexical_value ASC
                """;
        return entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .setParameter("lang", language)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<CandidatNoteDetailRow> findNotes(String thesaurusId, String conceptId) {
        String sql = """
                SELECT n.id, n.notetypecode, n.id_concept, n.lang, n.lexicalvalue, n.id_user
                FROM note n
                WHERE n.identifier = :conceptId
                  AND n.id_thesaurus = :thesaurusId
                ORDER BY n.id
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getResultList();
        return rows.stream()
                .map(row -> new CandidatNoteDetailRow(
                        ((Number) row[0]).intValue(),
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? (String) row[2] : "",
                        row[3] != null ? (String) row[3] : "",
                        row[4] != null ? (String) row[4] : "",
                        row[5] != null ? ((Number) row[5]).intValue() : null
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<CandidatTranslationRow> findTranslations(String thesaurusId, String conceptId, String lang) {
        String language = StringUtils.isBlank(lang) ? "fr" : lang;
        String sql = """
                SELECT t.lang, t.lexical_value, COALESCE(l.code_pays, '')
                FROM term t
                JOIN preferred_term pt
                    ON pt.id_term = t.id_term
                    AND pt.id_thesaurus = t.id_thesaurus
                JOIN languages_iso639 l ON t.lang = l.iso639_1
                WHERE pt.id_concept = :conceptId
                  AND t.id_thesaurus = :thesaurusId
                  AND t.lang <> :lang
                ORDER BY t.lexical_value
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .setParameter("lang", language)
                .getResultList();
        return rows.stream()
                .map(row -> new CandidatTranslationRow(
                        (String) row[0],
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? (String) row[2] : ""
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<CandidatDiscussionRow> findDiscussionMessages(String thesaurusId, String conceptId) {
        String sql = """
                SELECT u.id_user, u.username, c.value, c.date
                FROM candidat_messages c
                JOIN users u ON c.id_user = u.id_user
                WHERE c.id_concept = :conceptId
                  AND c.id_thesaurus = :thesaurusId
                ORDER BY c.date
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .getResultList();
        return rows.stream()
                .map(row -> new CandidatDiscussionRow(
                        ((Number) row[0]).intValue(),
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? (String) row[2] : "",
                        row[3] != null ? String.valueOf(row[3]) : ""
                ))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public Set<String> findNoteVoteIdsForUser(String thesaurusId, String conceptId, int userId) {
        String sql = """
                SELECT cv.id_note
                FROM candidat_vote cv
                WHERE cv.id_concept = :conceptId
                  AND cv.id_thesaurus = :thesaurusId
                  AND cv.id_user = :userId
                  AND cv.type_vote = 'NT'
                """;
        List<String> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .setParameter("userId", userId)
                .getResultList();
        return new HashSet<>(rows);
    }

    public boolean hasCandidateVoteForUser(String thesaurusId, String conceptId, int userId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM candidat_vote cv
                    WHERE cv.id_concept = :conceptId
                      AND cv.id_thesaurus = :thesaurusId
                      AND cv.id_user = :userId
                      AND cv.type_vote = 'CA'
                )
                """;
        return Boolean.TRUE.equals(entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .setParameter("userId", userId)
                .getSingleResult());
    }

    @SuppressWarnings("unchecked")
    private List<CandidatIdValueRow> findRelations(String thesaurusId, String conceptId, String lang, boolean broader) {
        String language = StringUtils.isBlank(lang) ? "fr" : lang;
        String sql = broader ? """
                SELECT hr.id_concept2, COALESCE(pref.lexical_value, '') AS label
                FROM hierarchical_relationship hr
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = hr.id_concept2
                    AND pt.id_thesaurus = hr.id_thesaurus
                LEFT JOIN term pref
                    ON pref.id_term = pt.id_term
                    AND pref.id_thesaurus = pt.id_thesaurus
                    AND pref.lang = :lang
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 = :conceptId
                  AND hr.role LIKE 'BT%'
                ORDER BY label
                """ : """
                SELECT hr.id_concept2, COALESCE(pref.lexical_value, '') AS label
                FROM hierarchical_relationship hr
                JOIN concept c
                    ON hr.id_concept2 = c.id_concept
                    AND hr.id_thesaurus = c.id_thesaurus
                LEFT JOIN preferred_term pt
                    ON pt.id_concept = hr.id_concept2
                    AND pt.id_thesaurus = hr.id_thesaurus
                LEFT JOIN term pref
                    ON pref.id_term = pt.id_term
                    AND pref.id_thesaurus = pt.id_thesaurus
                    AND pref.lang = :lang
                WHERE hr.id_thesaurus = :thesaurusId
                  AND hr.id_concept1 = :conceptId
                  AND hr.role = 'RT'
                  AND c.status != 'CA'
                ORDER BY label
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("conceptId", conceptId)
                .setParameter("lang", language)
                .getResultList();
        return rows.stream()
                .map(row -> new CandidatIdValueRow((String) row[0], row[1] != null ? (String) row[1] : ""))
                .toList();
    }

    private CandidatListRow toCandidatListRow(Object[] row) {
        return new CandidatListRow(
                (String) row[0],
                toLocalDateTime(row[1]),
                toLocalDateTime(row[2]),
                row[3] != null ? ((Number) row[3]).intValue() : 0,
                row[4] != null ? ((Number) row[4]).intValue() : null,
                row[5] != null ? (String) row[5] : "",
                row[6] != null ? (String) row[6] : "",
                row[7] != null ? (String) row[7] : UNKNOWN_USER,
                row[8] != null ? (String) row[8] : UNKNOWN_USER,
                row[9] != null ? ((Number) row[9]).intValue() : 0,
                row[10] != null ? ((Number) row[10]).intValue() : 0,
                row[11] != null ? ((Number) row[11]).intValue() : 0,
                row[12] != null ? ((Number) row[12]).intValue() : 0
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        return null;
    }
}
