package fr.cnrs.opentheso.shared.repository;

import fr.cnrs.opentheso.concept.model.ConceptFicheRows;
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

    /** {@code null} tant que le sondage des notes en URI n'a pas été fait. */
    private volatile Boolean uriShapedNotes;

    public Optional<Object[]> findConceptCore(String conceptId, String thesaurusId) {
        return findConceptCore(conceptId, thesaurusId, false);
    }

    public Optional<Object[]> findConceptCore(String conceptId, String thesaurusId, boolean includeCandidates) {
        List<Object[]> rows = em.createNativeQuery("""
            SELECT c.id_concept, c.status, c.id_ark, c.id_handle, c.id_doi,
                   COALESCE(c.notation, '') AS notation,
                   c.created::date, c.modified::date,
                   COALESCE(c.concept_type, '') AS concept_type
            FROM concept c
            WHERE c.id_concept = :conceptId
              AND c.id_thesaurus = :thesaurusId
              AND (:includeCandidates = true OR c.status <> 'CA')
            LIMIT 1
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("includeCandidates", includeCandidates)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.HIDDEN, hidden)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter(NativeQueryParams.HIDDEN, hidden)
                .getResultList();
    }

    public List<Object[]> findAllConceptNotes(String conceptId, String thesaurusId) {
        String sql = """
            SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
            FROM note n
            WHERE n.id_thesaurus = :thesaurusId AND n.id_concept = :conceptId
            UNION
            SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
            FROM note n
            WHERE n.id_thesaurus = :thesaurusId AND n.identifier = :conceptId
            UNION
            SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
            FROM note n
            WHERE n.id_thesaurus = :thesaurusId AND n.id_term = :conceptId
            UNION
            SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
            FROM note n
            WHERE n.id_thesaurus = :thesaurusId
              AND n.id_term IN (
                  SELECT pt.id_term FROM preferred_term pt
                  WHERE pt.id_concept = :conceptId AND pt.id_thesaurus = :thesaurusId
              )
            UNION
            SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
            FROM note n
            WHERE n.id_thesaurus = :thesaurusId
              AND n.identifier IN (
                  SELECT c.id_ark FROM concept c
                  WHERE c.id_concept = :conceptId
                    AND c.id_thesaurus = :thesaurusId
                    AND c.id_ark IS NOT NULL
                    AND btrim(c.id_ark) <> ''
              )
            """;
        if (uriShapedNotesPresent()) {
            sql += """
                UNION
                SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
                FROM note n
                WHERE n.id_thesaurus = :thesaurusId
                  AND (
                    n.identifier LIKE '%/' || :conceptId
                    OR n.id_concept LIKE '%/' || :conceptId
                    OR n.id_term LIKE '%/' || :conceptId
                    OR n.identifier LIKE '%idc=' || :conceptId || '&%'
                    OR n.identifier LIKE '%idf=' || :conceptId || '&%'
                    OR n.id_concept LIKE '%idc=' || :conceptId || '&%'
                    OR n.id_concept LIKE '%idf=' || :conceptId || '&%'
                    OR n.id_term LIKE '%idc=' || :conceptId || '&%'
                    OR n.id_term LIKE '%idf=' || :conceptId || '&%'
                    OR n.identifier LIKE '%idc=' || :conceptId
                    OR n.identifier LIKE '%idf=' || :conceptId
                    OR n.id_concept LIKE '%idc=' || :conceptId
                    OR n.id_concept LIKE '%idf=' || :conceptId
                    OR n.id_term LIKE '%idc=' || :conceptId
                    OR n.id_term LIKE '%idf=' || :conceptId
                  )
                """;
        }
        sql += " ORDER BY 2, 4";
        return em.createNativeQuery(sql)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
    }

    /**
     * Les notes importées avec une URI ne se trouvent que par un filtre {@code LIKE}.
     * Ce filtre empêche l'usage des index : on ne l'ajoute que si la base en contient.
     */
    private boolean uriShapedNotesPresent() {
        Boolean cached = uriShapedNotes;
        if (cached != null) {
            return cached;
        }
        boolean present = !em.createNativeQuery("""
                SELECT 1
                FROM note
                WHERE identifier LIKE '%/%' OR identifier LIKE '%idc=%' OR identifier LIKE '%idf=%'
                   OR id_concept LIKE '%/%' OR id_concept LIKE '%idc=%' OR id_concept LIKE '%idf=%'
                   OR id_term LIKE '%/%' OR id_term LIKE '%idc=%' OR id_term LIKE '%idf=%'
                LIMIT 1
                """).getResultList().isEmpty();
        uriShapedNotes = present;
        return present;
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
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND c.status != 'CA'
              AND hr.role LIKE 'BT%'
              AND (
                  :includePrivateGroups = true
                  OR NOT EXISTS (
                      SELECT 1
                      FROM concept_group_concept cgc
                      JOIN concept_group cg
                          ON cg.idgroup = cgc.idgroup
                          AND cg.idthesaurus = cgc.idthesaurus
                          AND cg.private = true
                      WHERE cgc.idconcept = hr.id_concept2
                        AND cgc.idthesaurus = hr.id_thesaurus
                  )
                  OR EXISTS (
                      SELECT 1
                      FROM concept_group_concept cgc
                      JOIN concept_group cg
                          ON cg.idgroup = cgc.idgroup
                          AND cg.idthesaurus = cgc.idthesaurus
                      WHERE cgc.idconcept = hr.id_concept2
                        AND cgc.idthesaurus = hr.id_thesaurus
                        AND COALESCE(cg.private, false) = false
                  )
              )
            ORDER BY sort_key, COALESCE(t.lexical_value, hr.id_concept2)
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    /**
     * Fils directs affichés dans l'arbre : mêmes filtres que {@code findTreeChildConcepts}
     * (candidats selon le visiteur, concepts sans libellé conservés, membres de facette exclus).
     * Le plafond de 4000 est celui de l'arbre. Le tri final est fait ensuite comme l'arbre.
     */
    public List<Object[]> findNarrowerRelations(
            String conceptId,
            String thesaurusId,
            String lang,
            boolean includeCandidates
    ) {
        return em.createNativeQuery("""
            SELECT COALESCE(t.lexical_value, c.id_concept) AS label,
                   c.id_concept,
                   hr.role,
                   c.id_ark, c.id_handle, c.id_doi,
                   COALESCE(c.notation, '') AS notation
            FROM hierarchical_relationship hr
            JOIN concept c
                ON c.id_concept = hr.id_concept2
                AND c.id_thesaurus = hr.id_thesaurus
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = c.id_thesaurus
                AND t.lang = :lang
            WHERE hr.id_thesaurus = :thesaurusId
              AND hr.id_concept1 = :conceptId
              AND hr.role LIKE 'NT%'
              AND (:includeCandidates = true OR UPPER(TRIM(c.status)) <> 'CA')
              AND NOT EXISTS (
                  SELECT 1
                  FROM concept_facet cf
                  JOIN thesaurus_array ta
                      ON cf.id_facet = ta.id_facet
                      AND cf.id_thesaurus = ta.id_thesaurus
                  WHERE cf.id_concept = c.id_concept
                    AND cf.id_thesaurus = c.id_thesaurus
                    AND ta.id_concept_parent = :conceptId
                    AND ta.id_thesaurus = :thesaurusId
              )
            ORDER BY c.notation, label
            LIMIT 4000
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter("includeCandidates", includeCandidates)
                .getResultList();
    }

    public List<Object[]> findAlignments(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT a.id, a.uri_target, a.alignement_id_type, COALESCE(a.thesaurus_target, '')
            FROM alignement a
            WHERE a.internal_id_concept = :conceptId
              AND a.internal_id_thesaurus = :thesaurusId
            ORDER BY a.alignement_id_type, a.uri_target
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.GROUP_ID, groupId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
        return firstScalarAsString(rows);
    }

    public List<Object[]> findImages(String conceptId, String thesaurusId) {
        return em.createNativeQuery("""
            SELECT ei.id,
                   CASE
                       WHEN ei.image_name IS NULL OR lower(btrim(ei.image_name)) = 'null' THEN ''
                       ELSE ei.image_name
                   END,
                   CASE
                       WHEN ei.image_copyright IS NULL
                            OR lower(btrim(ei.image_copyright)) = 'null'
                            OR btrim(ei.image_copyright) ~* '_(sparql|rest)$' THEN ''
                       ELSE ei.image_copyright
                   END,
                   ei.external_uri,
                   CASE
                       WHEN ei.image_creator IS NULL
                            OR lower(btrim(ei.image_creator)) = 'null'
                            OR btrim(ei.image_creator) ~* '_(sparql|rest)$' THEN ''
                       ELSE ei.image_creator
                   END
            FROM external_images ei
            WHERE ei.id_thesaurus = :thesaurusId
              AND ei.id_concept = :conceptId
            ORDER BY ei.id
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
    }

    public List<Object[]> findReplaces(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_ark, c.id_handle, c.id_doi, COALESCE(t.lexical_value, c.id_concept)
            FROM concept_replacedby cr
            JOIN concept c
                ON c.id_thesaurus = cr.id_thesaurus
                AND (
                    c.id_concept = cr.id_concept1
                    OR cr.id_concept1 LIKE '%/' || c.id_concept
                    OR (c.id_ark IS NOT NULL AND btrim(c.id_ark) <> '' AND (
                        c.id_ark = cr.id_concept1
                        OR c.id_ark = 'ark:/' || cr.id_concept1
                        OR c.id_ark LIKE '%/' || cr.id_concept1
                        OR cr.id_concept1 LIKE '%/' || c.id_ark
                    ))
                )
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
                AND t.lang = :lang
            WHERE cr.id_thesaurus = :thesaurusId
              AND (
                cr.id_concept2 = :conceptId
                OR cr.id_concept2 IN (
                    SELECT c0.id_ark FROM concept c0
                    WHERE c0.id_concept = :conceptId
                      AND c0.id_thesaurus = :thesaurusId
                      AND c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> ''
                    UNION
                    SELECT regexp_replace(c0.id_ark, '^.*/', '') FROM concept c0
                    WHERE c0.id_concept = :conceptId
                      AND c0.id_thesaurus = :thesaurusId
                      AND c0.id_ark IS NOT NULL AND position('/' in c0.id_ark) > 0
                )
                OR EXISTS (
                    SELECT 1 FROM concept c0
                    WHERE c0.id_concept = :conceptId
                      AND c0.id_thesaurus = :thesaurusId
                      AND (
                        cr.id_concept2 LIKE '%/' || c0.id_concept
                        OR (c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> ''
                            AND cr.id_concept2 LIKE '%/' || c0.id_ark)
                      )
                )
              )
            ORDER BY COALESCE(t.lexical_value, c.id_concept)
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .getResultList();
    }

    public List<Object[]> findReplacedBy(String conceptId, String thesaurusId, String lang) {
        return em.createNativeQuery("""
            SELECT c.id_concept, c.id_ark, c.id_handle, c.id_doi, COALESCE(t.lexical_value, c.id_concept)
            FROM concept_replacedby cr
            JOIN concept c
                ON c.id_thesaurus = cr.id_thesaurus
                AND (
                    c.id_concept = cr.id_concept2
                    OR cr.id_concept2 LIKE '%/' || c.id_concept
                    OR (c.id_ark IS NOT NULL AND btrim(c.id_ark) <> '' AND (
                        c.id_ark = cr.id_concept2
                        OR c.id_ark = 'ark:/' || cr.id_concept2
                        OR c.id_ark LIKE '%/' || cr.id_concept2
                        OR cr.id_concept2 LIKE '%/' || c.id_ark
                    ))
                )
            LEFT JOIN preferred_term pt
                ON pt.id_concept = c.id_concept
                AND pt.id_thesaurus = c.id_thesaurus
            LEFT JOIN term t
                ON t.id_term = pt.id_term
                AND t.id_thesaurus = pt.id_thesaurus
                AND t.lang = :lang
            WHERE cr.id_thesaurus = :thesaurusId
              AND (
                cr.id_concept1 = :conceptId
                OR cr.id_concept1 IN (
                    SELECT c0.id_ark FROM concept c0
                    WHERE c0.id_concept = :conceptId
                      AND c0.id_thesaurus = :thesaurusId
                      AND c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> ''
                    UNION
                    SELECT regexp_replace(c0.id_ark, '^.*/', '') FROM concept c0
                    WHERE c0.id_concept = :conceptId
                      AND c0.id_thesaurus = :thesaurusId
                      AND c0.id_ark IS NOT NULL AND position('/' in c0.id_ark) > 0
                )
                OR EXISTS (
                    SELECT 1 FROM concept c0
                    WHERE c0.id_concept = :conceptId
                      AND c0.id_thesaurus = :thesaurusId
                      AND (
                        cr.id_concept1 LIKE '%/' || c0.id_concept
                        OR (c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> ''
                            AND cr.id_concept1 LIKE '%/' || c0.id_ark)
                      )
                )
              )
            ORDER BY COALESCE(t.lexical_value, c.id_concept)
            """)
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
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
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .getResultList();
    }

    /**
     * Libellés, notes, relations, alignements et fil d'Ariane en une seule requête.
     * Chaque sous-requête séparée coûtait un aller-retour : la fiche enchaînait une vingtaine.
     */
    public Optional<ConceptFicheRows> loadFiche(
            String conceptId,
            String thesaurusId,
            String lang,
            boolean includePrivateGroups,
            boolean includeConceptCandidates,
            boolean includeNarrowerCandidates
    ) {
        Object raw = em.createNativeQuery(ficheSql(uriShapedNotesPresent()))
                .setParameter(NativeQueryParams.CONCEPT_ID, conceptId)
                .setParameter(NativeQueryParams.THESAURUS_ID, thesaurusId)
                .setParameter("lang", lang)
                .setParameter("includePrivateGroups", includePrivateGroups)
                .setParameter("includeConceptCandidates", includeConceptCandidates)
                .setParameter("includeNarrowerCandidates", includeNarrowerCandidates)
                .getSingleResult();
        ConceptFicheRows rows = ConceptFicheRows.parse(raw == null ? "{}" : raw.toString());
        return rows.core() == null ? Optional.empty() : Optional.of(rows);
    }

    private String ficheSql(boolean uriShapedNotes) {
        String notes = """
                SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '') AS notesource
                FROM note n
                WHERE n.id_thesaurus = :thesaurusId AND n.id_concept = :conceptId
                UNION
                SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
                FROM note n
                WHERE n.id_thesaurus = :thesaurusId AND n.identifier = :conceptId
                UNION
                SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
                FROM note n
                WHERE n.id_thesaurus = :thesaurusId AND n.id_term = :conceptId
                UNION
                SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
                FROM note n
                WHERE n.id_thesaurus = :thesaurusId
                  AND n.id_term IN (
                      SELECT pt.id_term FROM preferred_term pt
                      WHERE pt.id_concept = :conceptId AND pt.id_thesaurus = :thesaurusId
                  )
                UNION
                SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
                FROM note n
                WHERE n.id_thesaurus = :thesaurusId
                  AND n.identifier IN (
                      SELECT c.id_ark FROM concept c
                      WHERE c.id_concept = :conceptId
                        AND c.id_thesaurus = :thesaurusId
                        AND c.id_ark IS NOT NULL
                        AND btrim(c.id_ark) <> ''
                  )
                """;
        if (uriShapedNotes) {
            notes += """
                    UNION
                    SELECT n.id, n.notetypecode, n.lexicalvalue, n.lang, COALESCE(n.notesource, '')
                    FROM note n
                    WHERE n.id_thesaurus = :thesaurusId
                      AND (
                        n.identifier LIKE '%/' || :conceptId
                        OR n.id_concept LIKE '%/' || :conceptId
                        OR n.id_term LIKE '%/' || :conceptId
                        OR n.identifier LIKE '%idc=' || :conceptId || '&%'
                        OR n.identifier LIKE '%idf=' || :conceptId || '&%'
                        OR n.id_concept LIKE '%idc=' || :conceptId || '&%'
                        OR n.id_concept LIKE '%idf=' || :conceptId || '&%'
                        OR n.id_term LIKE '%idc=' || :conceptId || '&%'
                        OR n.id_term LIKE '%idf=' || :conceptId || '&%'
                        OR n.identifier LIKE '%idc=' || :conceptId
                        OR n.identifier LIKE '%idf=' || :conceptId
                        OR n.id_concept LIKE '%idc=' || :conceptId
                        OR n.id_concept LIKE '%idf=' || :conceptId
                        OR n.id_term LIKE '%idc=' || :conceptId
                        OR n.id_term LIKE '%idf=' || :conceptId
                      )
                    """;
        }
        return """
                SELECT CAST(jsonb_build_object(
                  'core', (
                    SELECT jsonb_build_array(c.id_concept, c.status, c.id_ark, c.id_handle, c.id_doi,
                           COALESCE(c.notation, ''), c.created::date, c.modified::date,
                           COALESCE(c.concept_type, ''))
                    FROM concept c
                    WHERE c.id_concept = :conceptId
                      AND c.id_thesaurus = :thesaurusId
                      AND (:includeConceptCandidates = true OR c.status <> 'CA')
                    LIMIT 1
                  ),
                  'creator', (
                    SELECT COALESCE(
                      (SELECT cd.value FROM concept_dcterms cd
                       WHERE cd.id_concept = :conceptId AND cd.id_thesaurus = :thesaurusId
                         AND cd.name = 'creator' LIMIT 1),
                      (SELECT u.username FROM concept c
                       JOIN users u ON u.id_user = c.creator
                       WHERE c.id_concept = :conceptId AND c.id_thesaurus = :thesaurusId
                         AND c.creator > 0 LIMIT 1),
                      ''
                    )
                  ),
                  'contributors', (
                    SELECT COALESCE(jsonb_agg(cd.value ORDER BY cd.value), '[]'::jsonb)
                    FROM concept_dcterms cd
                    WHERE cd.id_concept = :conceptId AND cd.id_thesaurus = :thesaurusId
                      AND cd.name = 'contributor'
                  ),
                  'preferred', (
                    SELECT jsonb_build_array(t.lexical_value, t.id_term, t.id)
                    FROM preferred_term pt
                    JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                    WHERE pt.id_concept = :conceptId AND pt.id_thesaurus = :thesaurusId AND t.lang = :lang
                    LIMIT 1
                  ),
                  'alts', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(npt.lexical_value, npt.id_term, npt.id)
                                              ORDER BY npt.lexical_value), '[]'::jsonb)
                    FROM non_preferred_term npt
                    JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                    WHERE pt.id_concept = :conceptId AND npt.id_thesaurus = :thesaurusId
                      AND npt.lang = :lang AND npt.hiden = false
                  ),
                  'hidden', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(npt.lexical_value, npt.id_term, npt.id)
                                              ORDER BY npt.lexical_value), '[]'::jsonb)
                    FROM non_preferred_term npt
                    JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                    WHERE pt.id_concept = :conceptId AND npt.id_thesaurus = :thesaurusId
                      AND npt.lang = :lang AND npt.hiden = true
                  ),
                  'prefTranslations', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(
                               t.id_term, t.lexical_value, t.lang, t.id, COALESCE(li.code_pays, ''))
                               ORDER BY t.lang), '[]'::jsonb)
                    FROM term t
                    JOIN preferred_term pt ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                    LEFT JOIN languages_iso639 li ON t.lang = li.iso639_1
                    WHERE t.id_thesaurus = :thesaurusId AND pt.id_concept = :conceptId AND t.lang != :lang
                  ),
                  'altTranslations', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(npt.id_term, npt.lexical_value, npt.lang, npt.id)
                                              ORDER BY npt.lang), '[]'::jsonb)
                    FROM non_preferred_term npt
                    JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                    WHERE pt.id_concept = :conceptId AND npt.id_thesaurus = :thesaurusId
                      AND npt.lang != :lang AND npt.hiden = false
                  ),
                  'hiddenTranslations', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(npt.id_term, npt.lexical_value, npt.lang, npt.id)
                                              ORDER BY npt.lang), '[]'::jsonb)
                    FROM non_preferred_term npt
                    JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                    WHERE pt.id_concept = :conceptId AND npt.id_thesaurus = :thesaurusId
                      AND npt.lang != :lang AND npt.hiden = true
                  ),
                  'notes', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(id, notetypecode, lexicalvalue, lang, notesource)
                                              ORDER BY notetypecode, lang), '[]'::jsonb)
                    FROM (
                """ + notes + """
                    ) notes
                  ),
                  'broaders', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(label, id_concept, role, id_ark, id_handle, id_doi, sort_key)), '[]'::jsonb)
                    FROM (
                      SELECT DISTINCT COALESCE(t.lexical_value, hr.id_concept2) AS label, hr.id_concept2 AS id_concept, hr.role,
                             c.id_ark, c.id_handle, c.id_doi,
                             CASE WHEN t.lexical_value ~ '^[0-9]+$' THEN t.lexical_value::int ELSE NULL END AS sort_key
                      FROM hierarchical_relationship hr
                      JOIN concept c ON hr.id_concept2 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
                      LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                      LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus AND t.lang = :lang
                      WHERE hr.id_thesaurus = :thesaurusId AND hr.id_concept1 = :conceptId
                        AND c.status != 'CA' AND hr.role LIKE 'BT%'
                        AND (
                          :includePrivateGroups = true
                          OR NOT EXISTS (
                            SELECT 1 FROM concept_group_concept cgc
                            JOIN concept_group cg ON cg.idgroup = cgc.idgroup AND cg.idthesaurus = cgc.idthesaurus AND cg.private = true
                            WHERE cgc.idconcept = hr.id_concept2 AND cgc.idthesaurus = hr.id_thesaurus
                          )
                          OR EXISTS (
                            SELECT 1 FROM concept_group_concept cgc
                            JOIN concept_group cg ON cg.idgroup = cgc.idgroup AND cg.idthesaurus = cgc.idthesaurus
                            WHERE cgc.idconcept = hr.id_concept2 AND cgc.idthesaurus = hr.id_thesaurus
                              AND COALESCE(cg.private, false) = false
                          )
                        )
                    ) broaders
                  ),
                  'relateds', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(label, id_concept, role, id_ark, id_handle, id_doi)
                                              ORDER BY label, role), '[]'::jsonb)
                    FROM (
                      SELECT COALESCE(t.lexical_value, hr.id_concept2) AS label, hr.id_concept2 AS id_concept, hr.role,
                             c.id_ark, c.id_handle, c.id_doi
                      FROM hierarchical_relationship hr
                      JOIN concept c ON hr.id_concept2 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
                      LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                      LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus AND t.lang = :lang
                      WHERE hr.id_thesaurus = :thesaurusId AND hr.id_concept1 = :conceptId
                        AND c.status != 'CA' AND hr.role LIKE 'RT%'
                    ) relateds
                  ),
                  'narrowers', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(label, id_concept, role, id_ark, id_handle, id_doi, notation)), '[]'::jsonb)
                    FROM (
                      SELECT COALESCE(t.lexical_value, c.id_concept) AS label, c.id_concept, hr.role,
                             c.id_ark, c.id_handle, c.id_doi, COALESCE(c.notation, '') AS notation
                      FROM hierarchical_relationship hr
                      JOIN concept c ON c.id_concept = hr.id_concept2 AND c.id_thesaurus = hr.id_thesaurus
                      LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                      LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = c.id_thesaurus AND t.lang = :lang
                      WHERE hr.id_thesaurus = :thesaurusId AND hr.id_concept1 = :conceptId AND hr.role LIKE 'NT%'
                        AND (:includeNarrowerCandidates = true OR UPPER(TRIM(c.status)) <> 'CA')
                        AND NOT EXISTS (
                          SELECT 1 FROM concept_facet cf
                          JOIN thesaurus_array ta ON cf.id_facet = ta.id_facet AND cf.id_thesaurus = ta.id_thesaurus
                          WHERE cf.id_concept = c.id_concept AND cf.id_thesaurus = c.id_thesaurus
                            AND ta.id_concept_parent = :conceptId AND ta.id_thesaurus = :thesaurusId
                        )
                      ORDER BY c.notation, label
                      LIMIT 4000
                    ) narrowers
                  ),
                  'alignments', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(a.id, a.uri_target, a.alignement_id_type, COALESCE(a.thesaurus_target, ''))
                                              ORDER BY a.alignement_id_type, a.uri_target), '[]'::jsonb)
                    FROM alignement a
                    WHERE a.internal_id_concept = :conceptId AND a.internal_id_thesaurus = :thesaurusId
                  ),
                  'gps', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(g.latitude, g.longitude, COALESCE(g.position, 0))
                                              ORDER BY g.position), '[]'::jsonb)
                    FROM gps g
                    WHERE g.id_theso = :thesaurusId AND g.id_concept = :conceptId
                  ),
                  'groups', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(idgroup, id_ark, id_handle, id_doi, label)
                                              ORDER BY label), '[]'::jsonb)
                    FROM (
                      SELECT cg.idgroup, cg.id_ark, cg.id_handle, cg.id_doi, COALESCE(cgl.lexicalvalue, cg.idgroup) AS label
                      FROM concept_group_concept cgc
                      JOIN concept_group cg ON cg.idgroup = cgc.idgroup AND cg.idthesaurus = cgc.idthesaurus
                      LEFT JOIN concept_group_label cgl ON cgl.idgroup = cg.idgroup AND cgl.idthesaurus = cg.idthesaurus AND cgl.lang = :lang
                      WHERE cgc.idthesaurus = :thesaurusId AND LOWER(cgc.idconcept) = LOWER(:conceptId)
                    ) groups
                  ),
                  'images', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(id, image_name, image_copyright, external_uri, image_creator)
                                              ORDER BY id), '[]'::jsonb)
                    FROM (
                      SELECT ei.id,
                             CASE WHEN ei.image_name IS NULL OR lower(btrim(ei.image_name)) = 'null' THEN '' ELSE ei.image_name END AS image_name,
                             CASE WHEN ei.image_copyright IS NULL OR lower(btrim(ei.image_copyright)) = 'null'
                                       OR btrim(ei.image_copyright) ~* '_(sparql|rest)$' THEN '' ELSE ei.image_copyright END AS image_copyright,
                             ei.external_uri,
                             CASE WHEN ei.image_creator IS NULL OR lower(btrim(ei.image_creator)) = 'null'
                                       OR btrim(ei.image_creator) ~* '_(sparql|rest)$' THEN '' ELSE ei.image_creator END AS image_creator
                      FROM external_images ei
                      WHERE ei.id_thesaurus = :thesaurusId AND ei.id_concept = :conceptId
                    ) images
                  ),
                  'replaces', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(id_concept, id_ark, id_handle, id_doi, label) ORDER BY label), '[]'::jsonb)
                    FROM (
                      SELECT c.id_concept, c.id_ark, c.id_handle, c.id_doi, COALESCE(t.lexical_value, c.id_concept) AS label
                      FROM concept_replacedby cr
                      JOIN concept c ON c.id_thesaurus = cr.id_thesaurus AND (
                        c.id_concept = cr.id_concept1
                        OR cr.id_concept1 LIKE '%/' || c.id_concept
                        OR (c.id_ark IS NOT NULL AND btrim(c.id_ark) <> '' AND (
                          c.id_ark = cr.id_concept1 OR c.id_ark = 'ark:/' || cr.id_concept1
                          OR c.id_ark LIKE '%/' || cr.id_concept1 OR cr.id_concept1 LIKE '%/' || c.id_ark))
                      )
                      LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                      LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus AND t.lang = :lang
                      WHERE cr.id_thesaurus = :thesaurusId AND (
                        cr.id_concept2 = :conceptId
                        OR cr.id_concept2 IN (
                          SELECT c0.id_ark FROM concept c0
                          WHERE c0.id_concept = :conceptId AND c0.id_thesaurus = :thesaurusId
                            AND c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> ''
                          UNION
                          SELECT regexp_replace(c0.id_ark, '^.*/', '') FROM concept c0
                          WHERE c0.id_concept = :conceptId AND c0.id_thesaurus = :thesaurusId
                            AND c0.id_ark IS NOT NULL AND position('/' in c0.id_ark) > 0
                        )
                        OR EXISTS (
                          SELECT 1 FROM concept c0
                          WHERE c0.id_concept = :conceptId AND c0.id_thesaurus = :thesaurusId AND (
                            cr.id_concept2 LIKE '%/' || c0.id_concept
                            OR (c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> '' AND cr.id_concept2 LIKE '%/' || c0.id_ark)
                          )
                        )
                      )
                    ) replaces
                  ),
                  'replacedBy', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(id_concept, id_ark, id_handle, id_doi, label) ORDER BY label), '[]'::jsonb)
                    FROM (
                      SELECT c.id_concept, c.id_ark, c.id_handle, c.id_doi, COALESCE(t.lexical_value, c.id_concept) AS label
                      FROM concept_replacedby cr
                      JOIN concept c ON c.id_thesaurus = cr.id_thesaurus AND (
                        c.id_concept = cr.id_concept2
                        OR cr.id_concept2 LIKE '%/' || c.id_concept
                        OR (c.id_ark IS NOT NULL AND btrim(c.id_ark) <> '' AND (
                          c.id_ark = cr.id_concept2 OR c.id_ark = 'ark:/' || cr.id_concept2
                          OR c.id_ark LIKE '%/' || cr.id_concept2 OR cr.id_concept2 LIKE '%/' || c.id_ark))
                      )
                      LEFT JOIN preferred_term pt ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                      LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus AND t.lang = :lang
                      WHERE cr.id_thesaurus = :thesaurusId AND (
                        cr.id_concept1 = :conceptId
                        OR cr.id_concept1 IN (
                          SELECT c0.id_ark FROM concept c0
                          WHERE c0.id_concept = :conceptId AND c0.id_thesaurus = :thesaurusId
                            AND c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> ''
                          UNION
                          SELECT regexp_replace(c0.id_ark, '^.*/', '') FROM concept c0
                          WHERE c0.id_concept = :conceptId AND c0.id_thesaurus = :thesaurusId
                            AND c0.id_ark IS NOT NULL AND position('/' in c0.id_ark) > 0
                        )
                        OR EXISTS (
                          SELECT 1 FROM concept c0
                          WHERE c0.id_concept = :conceptId AND c0.id_thesaurus = :thesaurusId AND (
                            cr.id_concept1 LIKE '%/' || c0.id_concept
                            OR (c0.id_ark IS NOT NULL AND btrim(c0.id_ark) <> '' AND cr.id_concept1 LIKE '%/' || c0.id_ark)
                          )
                        )
                      )
                    ) replaced_by
                  ),
                  'facets', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(cf.id_facet, COALESCE(nl.lexical_value, ''))
                                              ORDER BY COALESCE(NULLIF(nl.lexical_value, ''), cf.id_facet)), '[]'::jsonb)
                    FROM concept_facet cf
                    LEFT JOIN node_label nl ON nl.id_facet = cf.id_facet AND nl.id_thesaurus = cf.id_thesaurus AND nl.lang = :lang
                    WHERE cf.id_thesaurus = :thesaurusId AND cf.id_concept = :conceptId
                  ),
                  'externalResources', (
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(er.external_uri, COALESCE(er.description, ''))
                                              ORDER BY er.external_uri), '[]'::jsonb)
                    FROM external_resources er
                    WHERE er.id_thesaurus = :thesaurusId AND er.id_concept = :conceptId
                  ),
                  'ancestors', (
                    WITH RECURSIVE ancestors AS (
                      SELECT hr.id_concept1 AS child_id, hr.id_concept2 AS parent_id, 1 AS depth
                      FROM hierarchical_relationship hr
                      WHERE hr.id_concept1 = :conceptId AND hr.id_thesaurus = :thesaurusId AND hr.role LIKE 'BT%'
                      UNION ALL
                      SELECT hr.id_concept1, hr.id_concept2, a.depth + 1
                      FROM hierarchical_relationship hr
                      JOIN ancestors a ON hr.id_concept1 = a.parent_id
                      WHERE hr.id_thesaurus = :thesaurusId AND hr.role LIKE 'BT%' AND a.depth < 30
                    )
                    SELECT COALESCE(jsonb_agg(jsonb_build_array(child_id, parent_id, depth, parent_label) ORDER BY depth DESC), '[]'::jsonb)
                    FROM (
                      SELECT DISTINCT a.child_id, a.parent_id, a.depth,
                             COALESCE(t.lexical_value, a.parent_id) AS parent_label
                      FROM ancestors a
                      LEFT JOIN preferred_term pt ON pt.id_concept = a.parent_id AND pt.id_thesaurus = :thesaurusId
                      LEFT JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = :thesaurusId AND t.lang = :lang
                    ) ancestor_rows
                  )
                ) AS text)
                """;
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
