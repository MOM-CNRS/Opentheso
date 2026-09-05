package fr.cnrs.opentheso.v2.shared.repository;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.concept.model.ConceptLinkItem;
import fr.cnrs.opentheso.v2.concept.model.ThesaurusMetadataItem;
import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        return countByThesaurus(sql, thesaurusId);
    }

    /**
     * Quatre compteurs d'accueil en un aller-retour JDBC.
     * {@code pendingCandidates} = statut en attente uniquement.
     */
    public DashboardKpiRow countDashboardKpis(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return DashboardKpiRow.empty();
        }
        String sql = """
                SELECT
                    (SELECT COUNT(id_concept)
                     FROM concept
                     WHERE id_thesaurus = :thesaurusId
                       AND status NOT IN ('CA', 'DEP', 'dep'))::int,
                    (SELECT COUNT(*)
                     FROM candidat_status cs
                     JOIN concept c
                         ON cs.id_concept = c.id_concept
                        AND cs.id_thesaurus = c.id_thesaurus
                     WHERE cs.id_thesaurus = :thesaurusId
                       AND cs.id_status = :pendingStatus)::int,
                    (SELECT COUNT(*)
                     FROM concept_group
                     WHERE idthesaurus = :thesaurusId)::int,
                    (SELECT COUNT(*)
                     FROM thesaurus_label
                     WHERE id_thesaurus = :thesaurusId)::int
                """;
        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("pendingStatus", CandidatStatusCode.PENDING)
                .getSingleResult();
        return new DashboardKpiRow(number(row, 0), number(row, 1), number(row, 2), number(row, 3));
    }

    public int countConceptsWithoutDefinition(String thesaurusId) {
        String sql = """
                SELECT COUNT(*)
                FROM concept c
                LEFT JOIN (
                    SELECT n.identifier AS id_concept
                    FROM note n
                    WHERE n.id_thesaurus = :thesaurusId
                      AND n.notetypecode = 'definition'
                      AND n.lexicalvalue <> ''
                      AND n.identifier IS NOT NULL
                    UNION
                    SELECT n.id_concept
                    FROM note n
                    WHERE n.id_thesaurus = :thesaurusId
                      AND n.notetypecode = 'definition'
                      AND n.lexicalvalue <> ''
                      AND n.id_concept IS NOT NULL
                ) def ON def.id_concept = c.id_concept
                WHERE c.id_thesaurus = :thesaurusId
                  AND c.status NOT IN ('CA', 'DEP', 'dep')
                  AND def.id_concept IS NULL
                """;
        return countByThesaurus(sql, thesaurusId);
    }

    /**
     * Langues du thésaurus + nombre de concepts valides ayant une traduction
     * (libellé préféré) dans cette langue. Ne recalcule pas le total des concepts.
     */
    @SuppressWarnings("unchecked")
    public List<LanguageCoverageRow> findLanguageTranslationCoverage(String thesaurusId, String workLang) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        String lang = StringUtils.isBlank(workLang) ? "fr" : workLang;
        String sql = """
                SELECT l.iso639_1 AS code,
                       CASE WHEN :lang = 'fr' THEN l.french_name ELSE l.english_name END AS label,
                       COUNT(DISTINCT tr.id_concept) AS translated_count
                FROM thesaurus_label tl
                JOIN languages_iso639 l ON tl.lang = l.iso639_1
                LEFT JOIN (
                    SELECT t.lang, c.id_concept
                    FROM concept c
                    JOIN preferred_term pt
                        ON pt.id_concept = c.id_concept
                       AND pt.id_thesaurus = c.id_thesaurus
                    JOIN term t
                        ON t.id_term = pt.id_term
                       AND t.id_thesaurus = pt.id_thesaurus
                    WHERE c.id_thesaurus = :thesaurusId
                      AND c.status NOT IN ('CA', 'DEP', 'dep')
                      AND t.lexical_value <> ''
                      AND t.lang IS NOT NULL
                ) tr ON tr.lang = tl.lang
                WHERE tl.id_thesaurus = :thesaurusId
                GROUP BY l.iso639_1, l.french_name, l.english_name
                ORDER BY l.french_name
                """;
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.stream()
                .map(row -> new LanguageCoverageRow(
                        row[0] != null ? (String) row[0] : "",
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? ((Number) row[2]).intValue() : 0
                ))
                .filter(row -> StringUtils.isNotBlank(row.code()))
                .toList();
    }

    /**
     * Collections du thésaurus + nombre de concepts valides et de candidats rattachés.
     */
    @SuppressWarnings("unchecked")
    public List<CollectionCoverageRow> findCollectionMemberCoverage(String thesaurusId, String workLang) {
        return findCollectionMemberCoverage(thesaurusId, workLang, 0);
    }

    /**
     * @param limit si &gt; 0, coupe après {@code limit} collections (les plus peuplées)
     */
    @SuppressWarnings("unchecked")
    public List<CollectionCoverageRow> findCollectionMemberCoverage(
            String thesaurusId,
            String workLang,
            int limit
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        String lang = StringUtils.isBlank(workLang) ? "fr" : workLang;
        String sql = """
                SELECT cg.idgroup AS id,
                       COALESCE(MAX(cgl.lexicalvalue), cg.idgroup) AS label,
                       COUNT(c.id_concept) AS member_count
                FROM concept_group cg
                LEFT JOIN concept_group_label cgl
                    ON LOWER(cgl.idgroup) = LOWER(cg.idgroup)
                   AND cgl.idthesaurus = cg.idthesaurus
                   AND cgl.lang = :lang
                LEFT JOIN concept_group_concept cgc
                    ON LOWER(cgc.idgroup) = LOWER(cg.idgroup)
                   AND cgc.idthesaurus = cg.idthesaurus
                LEFT JOIN concept c
                    ON c.id_concept = cgc.idconcept
                   AND c.id_thesaurus = cgc.idthesaurus
                   AND c.status NOT IN ('DEP', 'dep')
                WHERE cg.idthesaurus = :thesaurusId
                GROUP BY cg.idgroup
                ORDER BY COUNT(c.id_concept) DESC, COALESCE(MAX(cgl.lexicalvalue), cg.idgroup)
                """;
        Query query = entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang);
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new CollectionCoverageRow(
                        row[0] != null ? (String) row[0] : "",
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? ((Number) row[2]).intValue() : 0
                ))
                .filter(row -> StringUtils.isNotBlank(row.id()))
                .toList();
    }

    public CandidateLifeStats findCandidateLifeStats(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return CandidateLifeStats.empty();
        }
        String countsSql = """
                SELECT
                    COUNT(*) FILTER (WHERE cs.id_status = 1)::int AS pending,
                    COUNT(*) FILTER (WHERE cs.id_status = 2)::int AS accepted,
                    COUNT(*) FILTER (WHERE cs.id_status = 3)::int AS rejected,
                    COUNT(*) FILTER (
                        WHERE cs.id_status = 2
                          AND COALESCE(c.modified::date, cs.date) >= CURRENT_DATE - INTERVAL '12 months'
                    )::int AS accepted_12m,
                    COUNT(*) FILTER (
                        WHERE cs.id_status = 3
                          AND COALESCE(c.modified::date, cs.date) >= CURRENT_DATE - INTERVAL '12 months'
                    )::int AS rejected_12m
                FROM candidat_status cs
                JOIN concept c
                    ON c.id_concept = cs.id_concept
                   AND c.id_thesaurus = cs.id_thesaurus
                WHERE cs.id_thesaurus = :thesaurusId
                """;
        Object[] counts = (Object[]) entityManager.createNativeQuery(countsSql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getSingleResult();
        int pending = number(counts, 0);
        int accepted = number(counts, 1);
        int rejected = number(counts, 2);
        int accepted12m = number(counts, 3);
        int rejected12m = number(counts, 4);

        String medianSql = """
                SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (
                    ORDER BY (COALESCE(c.modified::date, cs.date) - c.created::date)
                )
                FROM candidat_status cs
                JOIN concept c
                    ON c.id_concept = cs.id_concept
                   AND c.id_thesaurus = cs.id_thesaurus
                WHERE cs.id_thesaurus = :thesaurusId
                  AND cs.id_status = 2
                  AND c.created IS NOT NULL
                """;
        Number median = (Number) entityManager.createNativeQuery(medianSql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getSingleResult();

        String contributorsSql = """
                WITH recent AS (
                    SELECT cs.id_concept, cs.id_thesaurus, cs.id_user, cs.id_user_admin, cs.id_status
                    FROM candidat_status cs
                    JOIN concept c
                        ON c.id_concept = cs.id_concept
                       AND c.id_thesaurus = cs.id_thesaurus
                    WHERE cs.id_thesaurus = :thesaurusId
                      AND (
                          cs.date >= CURRENT_DATE - INTERVAL '12 months'
                          OR (
                              cs.id_status IN (2, 3)
                              AND COALESCE(c.modified::date, cs.date) >= CURRENT_DATE - INTERVAL '12 months'
                          )
                      )
                )
                SELECT COUNT(DISTINCT uid)
                FROM (
                    SELECT id_user AS uid FROM recent WHERE id_user IS NOT NULL
                    UNION
                    SELECT id_user_admin FROM recent WHERE id_user_admin IS NOT NULL AND id_status IN (2, 3)
                    UNION
                    SELECT cv.id_user
                    FROM candidat_vote cv
                    JOIN recent r
                        ON r.id_concept = cv.id_concept
                       AND r.id_thesaurus = cv.id_thesaurus
                    WHERE cv.id_user IS NOT NULL
                    UNION
                    SELECT cm.id_user
                    FROM candidat_messages cm
                    JOIN recent r
                        ON r.id_concept = cm.id_concept
                       AND r.id_thesaurus = cm.id_thesaurus
                    WHERE cm.id_user IS NOT NULL
                ) contributors
                """;
        Number contributors = (Number) entityManager.createNativeQuery(contributorsSql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getSingleResult();

        return new CandidateLifeStats(
                pending,
                accepted,
                rejected,
                accepted12m,
                rejected12m,
                median == null ? null : (int) Math.round(median.doubleValue()),
                contributors != null ? contributors.intValue() : 0
        );
    }

    /**
     * Propositions des 12 derniers mois, groupées par mois de dépôt et issue actuelle.
     */
    @SuppressWarnings("unchecked")
    public List<CandidateMonthRow> findCandidateMonthlyProposals(String thesaurusId) {
        Map<YearMonth, int[]> counts = new LinkedHashMap<>();
        if (StringUtils.isNotBlank(thesaurusId)) {
            String sql = """
                    SELECT to_char(date_trunc('month', COALESCE(cs.date, c.created::date)), 'YYYY-MM'),
                           COUNT(*) FILTER (WHERE cs.id_status = 2)::int,
                           COUNT(*) FILTER (WHERE cs.id_status = 1)::int,
                           COUNT(*) FILTER (WHERE cs.id_status = 3)::int
                    FROM candidat_status cs
                    JOIN concept c
                        ON c.id_concept = cs.id_concept
                       AND c.id_thesaurus = cs.id_thesaurus
                    WHERE cs.id_thesaurus = :thesaurusId
                      AND COALESCE(cs.date, c.created::date)
                          >= (date_trunc('month', CURRENT_DATE) - INTERVAL '11 months')::date
                    GROUP BY 1
                    """;
            List<Object[]> rows = entityManager.createNativeQuery(sql)
                    .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                    .getResultList();
            for (Object[] row : rows) {
                YearMonth month = parseYearMonth(row[0]);
                if (month == null) {
                    continue;
                }
                counts.put(month, new int[]{number(row, 1), number(row, 2), number(row, 3)});
            }
        }
        YearMonth current = V2Dates.nowYearMonth();
        List<CandidateMonthRow> months = new ArrayList<>(12);
        for (int i = 11; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            int[] values = counts.getOrDefault(month, new int[]{0, 0, 0});
            months.add(new CandidateMonthRow(month, values[0], values[1], values[2]));
        }
        return months;
    }

    private static YearMonth parseYearMonth(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.length() >= 7) {
            try {
                return YearMonth.parse(text.substring(0, 7));
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static int number(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return 0;
        }
        return ((Number) row[index]).intValue();
    }

    public int findMaxTreeDepth(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return 0;
        }
        String sql = """
                WITH RECURSIVE
                valid AS (
                    SELECT id_concept
                    FROM concept
                    WHERE id_thesaurus = :thesaurusId
                      AND status NOT IN ('CA', 'DEP', 'dep')
                ),
                edges AS (
                    SELECT hr.id_concept1 AS parent_id,
                           hr.id_concept2 AS child_id
                    FROM hierarchical_relationship hr
                    JOIN valid child ON child.id_concept = hr.id_concept2
                    WHERE hr.id_thesaurus = :thesaurusId
                      AND hr.role IN ('NT', 'NTG', 'NTP', 'NTI')
                ),
                tree AS (
                    SELECT c.id_concept, 1 AS depth
                    FROM concept c
                    WHERE c.id_thesaurus = :thesaurusId
                      AND c.top_concept = true
                      AND c.status NOT IN ('CA', 'DEP', 'dep')

                    UNION ALL

                    SELECT e.child_id, t.depth + 1
                    FROM tree t
                    JOIN edges e ON e.parent_id = t.id_concept
                    WHERE t.depth < 50
                )
                SELECT COALESCE(MAX(depth), 0)
                FROM tree
                """;
        Number depth = (Number) entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getSingleResult();
        return depth != null ? depth.intValue() : 0;
    }

    private int countByThesaurus(String sql, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return 0;
        }
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        List<ThesaurusMetadataItem> items = rows.stream()
                .map(row -> new ThesaurusMetadataItem(
                        ((Number) row[0]).intValue(),
                        row[1] != null ? (String) row[1] : "",
                        row[2] != null ? (String) row[2] : "",
                        row[3] != null ? (String) row[3] : "",
                        row[4] != null ? (String) row[4] : ""
                ))
                .toList();
        return dedupeSingularDcMetadata(items);
    }

    /**
     * Affiche une seule valeur pour {@code created}/{@code modified} (la plus récente).
     */
    static List<ThesaurusMetadataItem> dedupeSingularDcMetadata(List<ThesaurusMetadataItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        Map<String, ThesaurusMetadataItem> singularBest = new LinkedHashMap<>();
        List<ThesaurusMetadataItem> others = new ArrayList<>();
        for (ThesaurusMetadataItem item : items) {
            String name = item.name() == null ? "" : item.name().trim().toLowerCase();
            if ("created".equals(name) || "modified".equals(name)) {
                ThesaurusMetadataItem previous = singularBest.get(name);
                if (previous == null || isNewerDcDate(item.value(), previous.value())) {
                    singularBest.put(name, item);
                }
            } else {
                others.add(item);
            }
        }
        List<ThesaurusMetadataItem> result = new ArrayList<>(others.size() + singularBest.size());
        result.addAll(others);
        result.addAll(singularBest.values());
        return result;
    }

    private static boolean isNewerDcDate(String candidate, String current) {
        if (StringUtils.isBlank(candidate)) {
            return false;
        }
        if (StringUtils.isBlank(current)) {
            return true;
        }
        try {
            return Instant.parse(normalizeDcDate(candidate))
                    .isAfter(Instant.parse(normalizeDcDate(current)));
        } catch (Exception ignored) {
            return candidate.compareTo(current) > 0;
        }
    }

    private static String normalizeDcDate(String value) {
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return trimmed + "T00:00:00Z";
        }
        if (trimmed.endsWith("Z") || trimmed.contains("+") || trimmed.matches(".*[+-]\\d{2}:\\d{2}$")) {
            return trimmed;
        }
        return trimmed + "Z";
    }

    @SuppressWarnings("unchecked")
    public List<ConceptLinkItem> findLastModifiedConcepts(String thesaurusId, String lang) {
        return findLastModifiedConceptsBundle(thesaurusId, lang).concepts();
    }

    /**
     * Last modified concepts + most recent modification date in a single ordered query.
     */
    @SuppressWarnings("unchecked")
    public LastModifiedConceptsBundle findLastModifiedConceptsBundle(String thesaurusId, String lang) {
        String sql = """
                SELECT c.id_concept, t.lexical_value, c.modified
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
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        if (rows.isEmpty()) {
            return LastModifiedConceptsBundle.empty();
        }
        Date lastModified = toDate(rows.get(0)[2]);
        List<ConceptLinkItem> concepts = rows.stream()
                .map(row -> new ConceptLinkItem(
                        row[0] != null ? (String) row[0] : "",
                        row[1] != null ? (String) row[1] : ""
                ))
                .filter(item -> StringUtils.isNotBlank(item.label()))
                .toList();
        return new LastModifiedConceptsBundle(lastModified, concepts);
    }

    public record LastModifiedConceptsBundle(Date lastModified, List<ConceptLinkItem> concepts) {
        public static LastModifiedConceptsBundle empty() {
            return new LastModifiedConceptsBundle(null, Collections.emptyList());
        }
    }

    public record DashboardKpiRow(int concepts, int pendingCandidates, int collections, int languages) {
        public static DashboardKpiRow empty() {
            return new DashboardKpiRow(0, 0, 0, 0);
        }
    }

    public record LanguageCoverageRow(String code, String label, int translatedCount) {
    }

    public record CollectionCoverageRow(String id, String label, int memberCount) {
    }

    public record CandidateMonthRow(YearMonth month, int accepted, int pending, int rejected) {
        public int total() {
            return accepted + pending + rejected;
        }
    }

    public record CandidateLifeStats(
            int pending,
            int accepted,
            int rejected,
            int acceptedLast12Months,
            int rejectedLast12Months,
            Integer medianDecisionDays,
            int activeContributors
    ) {
        public static CandidateLifeStats empty() {
            return new CandidateLifeStats(0, 0, 0, 0, 0, null, 0);
        }

        public int acceptanceRatePercent() {
            int total = pending + accepted + rejected;
            if (total <= 0) {
                return 0;
            }
            return (int) Math.round(accepted * 100.0 / total);
        }
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
