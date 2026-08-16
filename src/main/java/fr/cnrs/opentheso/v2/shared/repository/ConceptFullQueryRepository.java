package fr.cnrs.opentheso.v2.shared.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Requêtes SQL natives pour charger une fiche concept complète, sans procédure {@code opentheso_get_concept}.
 */
@Repository
@Transactional(readOnly = true)
public class ConceptFullQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Object[]> findConceptCore(String conceptId, String thesaurusId) {
        return findConceptCore(conceptId, thesaurusId, false);
    }

    public Optional<Object[]> findConceptCore(String conceptId, String thesaurusId, boolean includeCandidates) {
        String statusFilter = includeCandidates ? "" : "AND c.status != 'CA'";
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept, c.status, c.id_ark, c.id_handle, c.id_doi,
                   COALESCE(c.notation, '') AS notation,
                   c.created::date, c.modified::date,
                   COALESCE(c.concept_type, '') AS concept_type
            FROM concept c
            WHERE c.id_concept = :conceptId
              AND c.id_thesaurus = :thesaurusId
            """ + statusFilter + """
            LIMIT 1
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<Object[]> findPreferredLabel(String conceptId, String thesaurusId, String lang) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT t.lexical_value, t.id_term, t.id
            FROM preferred_term pt
            JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
            WHERE pt.id_concept = :conceptId
              AND pt.id_thesaurus = :thesaurusId
              AND t.lang = :lang
            LIMIT 1
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<String> findConceptPreferredLabel(String conceptId, String thesaurusId, String lang) {
        return findPreferredLabel(conceptId, thesaurusId, lang)
                .map(row -> row[0] != null ? row[0].toString() : "");
    }

    public List<Object[]> findAltLabels(String conceptId, String thesaurusId, String lang, boolean hidden) {
        return em.createNativeQuery("""
            SELECT npt.lexical_value, npt.id_term, npt.id
            FROM non_preferred_term npt
            JOIN preferred_term pt
                ON pt.id_term = npt.id_term
                AND pt.id_thesaurus = npt.id_thesaurus
            WHERE pt.id_concept = :conceptId
              AND npt.id_thesaurus = :thesaurusId
              AND npt.lang = :lang
              AND npt.hiden = :hidden
            ORDER BY npt.lexical_value
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("hidden", hidden)
                .getResultList();
    }

    public List<Object[]> findPrefLabelTranslations(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT t.id_term, t.lexical_value, t.lang, t.id, COALESCE(li.code_pays, '')
            FROM term t
            JOIN preferred_term pt
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
            LEFT JOIN languages_iso639 li
                ON t.lang = li.iso639_1
            WHERE t.id_thesaurus = :thesaurusId
              AND pt.id_concept = :conceptId
              AND t.lang != :lang
            ORDER BY t.lang
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findAltLabelTranslations(String conceptId, String thesaurusId, String lang, boolean hidden) {
        return em.createNativeQuery("""
            SELECT npt.id_term, npt.lexical_value, npt.lang, npt.id
            FROM non_preferred_term npt
            JOIN preferred_term pt
                ON pt.id_term = npt.id_term
                AND pt.id_thesaurus = npt.id_thesaurus
            WHERE pt.id_concept = :conceptId
              AND npt.id_thesaurus = :thesaurusId
              AND npt.lang != :lang
              AND npt.hiden = :hidden
            ORDER BY npt.lang
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("hidden", hidden)
                .getResultList();
    }

    public List<Object[]> findAllConceptNotes(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
            FROM note n
            WHERE n.id_thesaurus = :thesaurusId
              AND n.identifier = :conceptId
            ORDER BY n.notetypecode, n.lang
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> findBroaderRelations(
            String conceptId,
            String thesaurusId,
            String lang,
            boolean includePrivateGroups
    ) {
        return em.createNativeQuery("""
            SELECT DISTINCT COALESCE(t.lexical_value, hr.id_concept2), hr.id_concept2, hr.role,
                   c.id_ark, c.id_handle, c.id_doi,
                   CASE WHEN t.lexical_value ~ '^[0-9]+$' THEN t.lexical_value::int ELSE NULL END AS sort_key
            FROM hierarchical_relationship hr
            JOIN concept c
                ON hr.id_concept2 = c.id_concept
                AND hr.id_thesaurus = c.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
                AND t.lang = :lang
            LEFT JOIN concept_group_concept cgc
                ON cgc.idconcept = hr.id_concept2
                AND cgc.idthesaurus = hr.id_thesaurus
            LEFT JOIN concept_group cg
                ON cg.idgroup = cgc.idgroup
                AND cg.idthesaurus = cgc.idthesaurus
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND c.status != 'CA'
              AND hr.role LIKE 'BT%'
              AND (
                  :includePrivateGroups = true
                  OR cg.private = false
                  OR cg.private IS NULL
              )
            ORDER BY sort_key, COALESCE(t.lexical_value, hr.id_concept2)
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("includePrivateGroups", includePrivateGroups)
                .getResultList();
    }

    public List<Object[]> findRelatedRelations(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT COALESCE(t.lexical_value, hr.id_concept2), hr.id_concept2, hr.role,
                   c.id_ark, c.id_handle, c.id_doi
            FROM hierarchical_relationship hr
            JOIN concept c
                ON hr.id_concept2 = c.id_concept
                AND hr.id_thesaurus = c.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
                AND t.lang = :lang
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND c.status != 'CA'
              AND hr.role LIKE 'RT%'
            ORDER BY COALESCE(t.lexical_value, hr.id_concept2), hr.role
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findNarrowerRelations(
            String conceptId,
            String thesaurusId,
            String lang,
            boolean includePrivateGroups,
            int offset,
            int limit
    ) {
        var query = em.createNativeQuery("""
            SELECT DISTINCT t.lexical_value, hr.id_concept2, hr.role, c.id_ark, c.id_handle, c.id_doi,
                   CASE WHEN t.lexical_value ~ '^[0-9]+$' THEN t.lexical_value::int ELSE NULL END AS sort_key
            FROM hierarchical_relationship hr
            JOIN concept c
                ON hr.id_concept2 = c.id_concept
                AND hr.id_thesaurus = c.id_thesaurus
            JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            JOIN term t
                ON t.id_term = pt.id_term
                AND t.lang = :lang
            LEFT JOIN concept_group_concept cgc
                ON cgc.idconcept = c.id_concept
                AND cgc.idthesaurus = c.id_thesaurus
            LEFT JOIN concept_group cg
                ON cg.idgroup = cgc.idgroup
                AND cg.idthesaurus = cgc.idthesaurus
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND c.status != 'CA'
              AND hr.role LIKE 'NT%'
              AND (
                  :includePrivateGroups = true
                  OR cg.private IS NULL
                  OR cg.private = false
              )
            ORDER BY sort_key, t.lexical_value ASC
            OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .setParameter("includePrivateGroups", includePrivateGroups)
                .setParameter("offset", offset)
                .setParameter("limit", limit);
        return query.getResultList();
    }

    public List<Object[]> findAlignments(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT a.id, a.uri_target, a.alignement_id_type, COALESCE(a.thesaurus_target, '')
            FROM alignement a
            WHERE a.internal_id_concept = :conceptId
              AND a.internal_id_thesaurus = :thesaurusId
            ORDER BY a.alignement_id_type, a.uri_target
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> findGpsPoints(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT g.latitude, g.longitude, COALESCE(g.position, 0)
            FROM gps g
            WHERE g.id_theso = :thesaurusId
              AND g.id_concept = :conceptId
            ORDER BY g.position
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> findGroupMemberships(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT cg.idgroup, cg.id_ark, cg.id_handle, cg.id_doi, COALESCE(cgl.lexicalvalue, cg.idgroup)
            FROM concept_group_concept cgc
            JOIN concept_group cg
                ON cg.idgroup = cgc.idgroup
                AND cg.idthesaurus = cgc.idthesaurus
            LEFT JOIN concept_group_label cgl
                ON cgl.idgroup = cg.idgroup
                AND cgl.idthesaurus = cg.idthesaurus
                AND cgl.lang = :lang
            WHERE cgc.idthesaurus = :thesaurusId
              AND LOWER(cgc.idconcept) = LOWER(:conceptId)
            ORDER BY COALESCE(cgl.lexicalvalue, cg.idgroup)
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public Optional<String> findGroupLabel(String groupId, String thesaurusId, String lang) {
        List<?> rows = em.createNativeQuery("""
            SELECT cgl.lexicalvalue
            FROM concept_group_label cgl
            WHERE cgl.idthesaurus = :thesaurusId
              AND cgl.idgroup = :groupId
              AND cgl.lang = :lang
            LIMIT 1
            """)
                .setParameter("groupId", groupId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return firstScalarAsString(rows);
    }

    public List<Object[]> findImages(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT ei.id, ei.image_name, ei.image_copyright, ei.external_uri, COALESCE(ei.image_creator, '')
            FROM external_images ei
            WHERE ei.id_thesaurus = :thesaurusId
              AND ei.id_concept = :conceptId
            ORDER BY ei.id
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    /**
     * Créateur affiché : d'abord {@code concept_dcterms} (name=creator), sinon
     * le username lié à {@code concept.creator} (cas des concepts sans ligne DC).
     */
    public Optional<String> findCreator(String conceptId, String thesaurusId) {
        List<?> rows = em.createNativeQuery("""
            SELECT COALESCE(
                (
                    SELECT cd.value
                    FROM concept_dcterms cd
                    WHERE cd.id_concept = :conceptId
                      AND cd.id_thesaurus = :thesaurusId
                      AND cd.name = 'creator'
                    LIMIT 1
                ),
                (
                    SELECT u.username
                    FROM concept c
                    JOIN users u ON u.id_user = c.creator
                    WHERE c.id_concept = :conceptId
                      AND c.id_thesaurus = :thesaurusId
                      AND c.creator > 0
                    LIMIT 1
                ),
                ''
            )
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
        return firstScalarAsString(rows).filter(StringUtils::isNotBlank);
    }

    public List<String> findContributors(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT cd.value
            FROM concept_dcterms cd
            WHERE cd.id_concept = :conceptId
              AND cd.id_thesaurus = :thesaurusId
              AND cd.name = 'contributor'
            ORDER BY cd.value
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    public List<Object[]> findReplaces(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_ark, c.id_handle, c.id_doi, COALESCE(t.lexical_value, c.id_concept)
            FROM concept_replacedby cr
            JOIN concept c
                ON c.id_concept = cr.id_concept1
                AND c.id_thesaurus = cr.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
                AND t.lang = :lang
            WHERE cr.id_concept2 = :conceptId
              AND cr.id_thesaurus = :thesaurusId
            ORDER BY COALESCE(t.lexical_value, c.id_concept)
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findReplacedBy(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_ark, c.id_handle, c.id_doi, COALESCE(t.lexical_value, c.id_concept)
            FROM concept_replacedby cr
            JOIN concept c
                ON c.id_concept = cr.id_concept2
                AND c.id_thesaurus = cr.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
                AND t.lang = :lang
            WHERE cr.id_concept1 = :conceptId
              AND cr.id_thesaurus = :thesaurusId
            ORDER BY COALESCE(t.lexical_value, c.id_concept)
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findFacets(String conceptId, String thesaurusId, String lang) {
        // Aligné legacy ConceptView.setFacetsOfConcept : facettes dont le concept est membre
        // (table concept_facet), pas les facettes dont il est parent (thesaurus_array).
        return em.createNativeQuery("""
            SELECT cf.id_facet, COALESCE(nl.lexical_value, '')
            FROM concept_facet cf
            LEFT JOIN node_label nl
                ON nl.id_facet = cf.id_facet
                AND nl.id_thesaurus = cf.id_thesaurus
                AND nl.lang = :lang
            WHERE cf.id_thesaurus = :thesaurusId
              AND cf.id_concept = :conceptId
            ORDER BY COALESCE(NULLIF(nl.lexical_value, ''), cf.id_facet)
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findExternalResources(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT er.external_uri, er.id_concept, COALESCE(er.description, '')
            FROM external_resources er
            WHERE er.id_thesaurus = :thesaurusId
              AND er.id_concept = :conceptId
            ORDER BY er.external_uri
            """)
                .setParameter("conceptId", conceptId)
                .setParameter("thesaurusId", thesaurusId)
                .getResultList();
    }

    private static Optional<String> firstScalarAsString(List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        Object value = rows.get(0);
        if (value instanceof Object[] values) {
            if (values.length == 0 || values[0] == null) {
                return Optional.empty();
            }
            return Optional.of(values[0].toString());
        }
        return Optional.of(value.toString());
    }
}
