package fr.cnrs.opentheso.repositories;

import fr.cnrs.opentheso.entites.Concept;
import fr.cnrs.opentheso.models.ConceptIdOnly;
import fr.cnrs.opentheso.models.NodeAutoCompletionProjection;
import fr.cnrs.opentheso.models.NodeSearchMiniAltProjection;
import fr.cnrs.opentheso.models.NodeSearchMiniProjection;
import fr.cnrs.opentheso.models.nodes.NodeIdValue;
import fr.cnrs.opentheso.models.search.NodeSearchMini;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchRepository extends JpaRepository<Concept, Integer> {

    // Recherche sur les termes préférés (avec concepts dépréciés)
    @Query(value = """
                SELECT pt.id_concept as id, t.lexical_value as value
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.status NOT IN ('CA', 'hidden')
                  AND t.lang = :idLang
                  AND t.id_thesaurus = :idThesaurus
                  AND f_unaccent(lower(t.lexical_value)) LIKE %:value%
                ORDER BY t.lexical_value <-> :value
                LIMIT 20
            """, nativeQuery = true)
    List<Object[]> searchPreferredLabels(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    // Recherche sur les termes non-préférés (avec concepts dépréciés)
    @Query(value = """
                SELECT non_preferred_term.lexical_value as npt, term.lexical_value as pt, preferred_term.id_concept
                FROM concept, preferred_term, non_preferred_term, term
                WHERE preferred_term.id_concept = concept.id_concept 
                      AND preferred_term.id_thesaurus = concept.id_thesaurus 
                      AND non_preferred_term.id_term = preferred_term.id_term 
                      AND non_preferred_term.id_thesaurus = preferred_term.id_thesaurus 
                      AND term.id_term = preferred_term.id_term 
                      AND term.id_thesaurus = preferred_term.id_thesaurus 
                      AND term.lang = non_preferred_term.lang 
                      AND concept.status NOT IN ('CA', 'hidden') 
                      AND non_preferred_term.id_thesaurus = :idThesaurus 
                      AND non_preferred_term.lang = :idLang 
                      AND f_unaccent(lower(non_preferred_term.lexical_value)) LIKE %:value%
                ORDER BY non_preferred_term.lexical_value <-> :value
                LIMIT 20
            """, nativeQuery = true)
    List<Object[]> searchAltLabels(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    // Recherche sur les termes non-préférés (avec concepts dépréciés)
    @Query(value = """
                SELECT pt.id_concept as id, npt.lexical_value || ' ->' || t.lexical_value as value
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN non_preferred_term npt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.status NOT IN ('CA', 'hidden')
                  AND npt.lang = :idLang
                  AND npt.id_thesaurus = :idThesaurus
                  AND f_unaccent(lower(npt.lexical_value)) LIKE %:value%
                ORDER BY npt.lexical_value <-> :value
                LIMIT 20
            """, nativeQuery = true)
    List<Object[]> searchAltLabelsWithDeprecated(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    // Recherche sur les termes préférés (sans concepts dépréciés)
    @Query(value = """
                SELECT pt.id_concept as id, t.lexical_value as value
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.status NOT IN ('CA', 'DEP', 'dep', 'hidden')
                  AND t.lang = :idLang
                  AND t.id_thesaurus = :idThesaurus
                  AND f_unaccent(lower(t.lexical_value)) LIKE %:value%
                ORDER BY t.lexical_value <-> :value
                LIMIT 20
            """, nativeQuery = true)
    List<Object[]> searchPreferredLabelsWithoutDeprecated(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    // Recherche sur les termes non-préférés (sans concepts dépréciés)
    @Query(value = """
                SELECT pt.id_concept as id, npt.lexical_value || ' ->' || t.lexical_value as value
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN non_preferred_term npt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.status NOT IN ('CA', 'DEP', 'dep', 'hidden')
                  AND npt.lang = :idLang
                  AND npt.id_thesaurus = :idThesaurus
                  AND f_unaccent(lower(npt.lexical_value)) LIKE %:value%
                ORDER BY npt.lexical_value <-> :value
                LIMIT 20
            """, nativeQuery = true)
    List<Object[]> searchAltLabelsWithoutDeprecated(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT pt.id_concept as id, t.lexical_value as value, c.concept_type
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.status NOT IN ('CA', 'hidden')
                  AND t.lang = :idLang
                  AND t.id_thesaurus = :idThesaurus
                  AND c.concept_type IS NOT NULL
                  AND c.concept_type != 'concept'
                  AND c.concept_type != ''
                  AND f_unaccent(lower(t.lexical_value)) LIKE %:value%
                ORDER BY t.lexical_value <-> :value
                LIMIT 20
            """, nativeQuery = true)
    List<Object[]> searchPreferredCustomRelations(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT pt.id_concept as id,\s
                       npt.lexical_value || ' ->' || t.lexical_value as label,
                       c.concept_type
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN non_preferred_term npt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.status NOT IN ('CA', 'hidden')
                  AND npt.lang = :idLang
                  AND npt.id_thesaurus = :idThesaurus
                  AND c.concept_type IS NOT NULL
                  AND c.concept_type != 'concept'
                  AND c.concept_type != ''
                  AND f_unaccent(lower(npt.lexical_value)) LIKE %:value%
                ORDER BY npt.lexical_value <-> :value
                LIMIT 20
            """, nativeQuery = true)
    List<Object[]> searchAltCustomRelations(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT DISTINCT term.lexical_value, term.lang, concept.id_concept, concept.status, term.id_term
                FROM concept
                JOIN concept_group_concept ON concept.id_concept = concept_group_concept.idconcept AND concept.id_thesaurus = concept_group_concept.idthesaurus
                JOIN preferred_term ON concept.id_concept = preferred_term.id_concept AND concept.id_thesaurus = preferred_term.id_thesaurus
                JOIN term ON preferred_term.id_term = term.id_term AND preferred_term.id_thesaurus = term.id_thesaurus
                WHERE term.id_thesaurus = :idThesaurus 
                  AND term.lang = :idLang
                  AND ((:tsQuery IS NOT NULL AND to_tsvector(CAST(:lang AS regconfig), term.lexical_value) @@ to_tsquery(CAST(:lang AS regconfig), :tsQuery)) OR (:likeQuery IS NOT NULL AND f_unaccent(lower(term.lexical_value)) LIKE :likeQuery))
                  AND (:groupsEmpty = true OR concept_group_concept.idgroup = ANY(:groups))
                ORDER BY term.lexical_value
                LIMIT 100
            """, nativeQuery = true)
    List<Object[]> searchConcepts(@Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang,
                                  @Param("tsQuery") String tsQuery, @Param("likeQuery") String likeQuery, @Param("lang") String lang,
                                  @Param("groups") String[] groups, @Param("groupsEmpty") boolean groupsEmpty);

    @Query(value = """
                SELECT DISTINCT c.id_concept
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept AND c.id_thesaurus = cgc.idthesaurus
                WHERE c.id_thesaurus = :idThesaurus
                  AND c.status NOT IN ('CA', 'DEP', 'dep')
                  AND (:idLang IS NULL OR t.lang = :idLang)
                  AND unaccent(lower(t.lexical_value)) = unaccent(lower(:value))
                  AND (:idGroups IS NULL OR LOWER(cgc.idgroup) IN (:idGroups))
            
                UNION
            
                SELECT DISTINCT c.id_concept
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN non_preferred_term npt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept AND c.id_thesaurus = cgc.idthesaurus
                WHERE c.id_thesaurus = :idThesaurus
                  AND c.status NOT IN ('CA', 'DEP', 'dep')
                  AND (:idLang IS NULL OR npt.lang = :idLang)
                  AND unaccent(lower(npt.lexical_value)) = unaccent(lower(:value))
                  AND (:idGroups IS NULL OR LOWER(cgc.idgroup) IN (:idGroups))
                LIMIT 100
            """, nativeQuery = true)
    List<String> searchConceptIdsByExactMatch(@Param("value") String value, @Param("idLang") String idLang,
                                              @Param("idGroups") List<String> idGroups, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT pt.id_concept, t.lexical_value, t.id_term, c.status
                FROM term t
                JOIN preferred_term pt ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                JOIN concept c ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                WHERE c.status != 'CA'
                  AND t.id_thesaurus = :idThesaurus
                  AND (:idLang IS NULL OR t.lang = :idLang)
                  AND similarity(unaccent(lower(t.lexical_value)), unaccent(lower(:value))) > 0.2
                ORDER BY similarity(unaccent(lower(t.lexical_value)), unaccent(lower(:value))) DESC
                LIMIT 50
            """, nativeQuery = true)
    List<Object[]> searchPreferredTermsLike2(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT pt.id_concept, t.id_term, npt.lexical_value AS npt, t.lexical_value AS pt, c.status
                FROM non_preferred_term npt
                JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
                JOIN concept c ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                WHERE c.status != 'CA'
                  AND npt.id_thesaurus = :idThesaurus
                  AND (:idLang IS NULL OR npt.lang = :idLang)
                  AND similarity(unaccent(lower(npt.lexical_value)), unaccent(lower(:value))) > 0.2
                ORDER BY similarity(unaccent(lower(npt.lexical_value)), unaccent(lower(:value))) DESC
                LIMIT 50
            """, nativeQuery = true)
    List<Object[]> searchNonPreferredTermsLike(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);


    // #MR validé par Miled le 13/11/2025 pour rechercher les notes
    @Query(value = """
            SELECT
                c.id_concept AS idConcept,
                t.lexical_value AS prefLabel,
                t.id_term AS idTerm,
                c.status AS status
            FROM
                concept c
            JOIN
                preferred_term pt
                ON c.id_concept = pt.id_concept
                AND c.id_thesaurus = pt.id_thesaurus
            JOIN
                term t
                ON pt.id_term = t.id_term
                AND pt.id_thesaurus = t.id_thesaurus
            JOIN
                note n
                ON c.id_concept = n.identifier
                AND c.id_thesaurus = n.id_thesaurus
                AND n.lang = t.lang
            WHERE
                n.lang = :idLang
                AND c.id_thesaurus = :idThesaurus
                AND (
                    unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT(:value, '%')))
                    OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('% ', :value, '%')))
                    OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%-', :value, '%')))
                    OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%_', :value, '%')))
                    OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%;', :value, '%')))
                    OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%:', :value, '%')))
                    OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%''', :value, '%')))
                    OR unaccent(lower(n.lexicalvalue)) LIKE unaccent(lower(CONCAT('%"', :value, '%')))
                )
            LIMIT 50;
            """, nativeQuery = true)
    List<NodeSearchMiniProjection> searchNotes(
            @Param("value") String value,
            @Param("idLang") String idLang,
            @Param("idThesaurus") String idThesaurus
    );

    @Query(value = """
            SELECT id_concept, lexical_value, id_term, status
                 FROM (
                     SELECT
                            pt.id_concept,
                            t.lexical_value,
                            t.id_term,
                            c.status,
                            unaccent(lower(t.lexical_value)) AS sort_norm
                     FROM preferred_term pt
                     JOIN term t
                         ON t.id_term = pt.id_term
                        AND t.id_thesaurus = pt.id_thesaurus
                     JOIN concept c
                         ON pt.id_concept = c.id_concept
                        AND pt.id_thesaurus = c.id_thesaurus
                     WHERE c.status != 'CA'
                       AND pt.id_thesaurus = :idThesaurus
                       AND (:idLang IS NULL OR t.lang = :idLang)
                       AND unaccent(lower(t.lexical_value)) ~* ('(^|[^[:alpha:]])' || unaccent(lower(:val)))
                    /*   AND (
                           unaccent(lower(t.lexical_value)) ILIKE CONCAT(unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('% ', unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('%-', unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('%_', unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('%;', unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('%''', unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('%ʿ', unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('%[', unaccent(lower(:val)), '%')
                           OR unaccent(lower(t.lexical_value)) ILIKE CONCAT('%(', unaccent(lower(:val)), '%')
                       )*/
                 ) x
                 ORDER BY x.sort_norm
                 LIMIT 50;
            """, nativeQuery = true)
    List<Object[]> searchStartWithPreferred(
            @Param("val") String val,
            @Param("idLang") String idLang,
            @Param("idThesaurus") String idThesaurus
    );


    @Query(value = """
                SELECT
                    pt.id_concept,
                    t.id_term,
                    npt.lexical_value AS npt,
                    t.lexical_value AS pt,
                    c.status
                FROM non_preferred_term npt
                JOIN preferred_term pt
                    ON pt.id_term = npt.id_term
                   AND pt.id_thesaurus = npt.id_thesaurus
                JOIN term t
                    ON t.id_term = pt.id_term
                   AND t.id_thesaurus = pt.id_thesaurus
                   AND t.lang = npt.lang
                JOIN concept c
                    ON c.id_concept = pt.id_concept
                   AND c.id_thesaurus = pt.id_thesaurus
                WHERE c.status != 'CA'
                  AND t.id_thesaurus = :idThesaurus
                  AND (:idLang IS NULL OR npt.lang = :idLang)
                  AND ' ' || LTRIM(
                        lower(
							unaccent(
	                          replace(
	                            replace(
	                              replace(
	                                replace(
	                                  replace(
	                                    replace(
	                                      replace(
	                                        replace(
	                                          npt.lexical_value, '[', ' '),
	                                        ']', ' '),
	                                      '(', ' '),
	                                    ')', ' '),
	                                  '~', ' '),
	                                '-', ' '),
	                              '''', ' '),  -- apostrophe
	                            '`', ' ')       -- backtick
	                        )
						)
                      ) || ' ' LIKE '% ' || lower(unaccent(:val)) || '%'
                    ORDER BY
                      CASE
                        WHEN unaccent(lower(npt.lexical_value)) ILIKE :val THEN 1
                        WHEN unaccent(lower(npt.lexical_value)) ILIKE CONCAT(:val, ' %') THEN 2
                      END,
                      unaccent(lower(npt.lexical_value))
                LIMIT 50
            """, nativeQuery = true)
    List<Object[]> searchNonPreferred(
            @Param("val") String val,
            @Param("idLang") String idLang,
            @Param("idThesaurus") String idThesaurus
    );

    @Query(value = """
            SELECT id_concept, id_term, npt, pt, status
                         FROM (
                             SELECT 
                                    pt.id_concept,
                                    t.id_term,
                                    npt.lexical_value AS npt,
                                    t.lexical_value   AS pt,
                                    c.status,
                                    unaccent(lower(npt.lexical_value)) AS sort_norm
                             FROM non_preferred_term npt
                             JOIN preferred_term pt
                                 ON pt.id_term = npt.id_term
                                AND pt.id_thesaurus = npt.id_thesaurus
                             JOIN term t
                                 ON t.id_term = pt.id_term
                                AND t.id_thesaurus = pt.id_thesaurus
                                AND t.lang = npt.lang
                             JOIN concept c
                                 ON pt.id_concept = c.id_concept
                                AND pt.id_thesaurus = c.id_thesaurus
                             WHERE c.status != 'CA'
                               AND npt.id_thesaurus = :idThesaurus
                               AND (:idLang IS NULL OR npt.lang = :idLang)
                               
                               AND unaccent(lower(npt.lexical_value)) ~* ('(^|[^[:alpha:]])' || unaccent(lower(:val)))
                               /*            
                               AND (
                                   unaccent(lower(npt.lexical_value)) ILIKE CONCAT(unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('% ', unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('%-', unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('%_', unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('%;', unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('%''', unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('%ʿ', unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('%[', unaccent(lower(:val)), '%')
                                   OR unaccent(lower(npt.lexical_value)) ILIKE CONCAT('%(', unaccent(lower(:val)), '%')
                               )*/
                               -- Exclure les concepts privés via NOT EXISTS
                               AND NOT EXISTS (
                                   SELECT 1
                                   FROM concept_group_concept cgc2
                                   JOIN concept_group cg2
                                     ON cgc2.idgroup = cg2.idgroup
                                    AND cgc2.idthesaurus = cg2.idthesaurus
                                   WHERE cgc2.idconcept = c.id_concept
                                     AND cgc2.idthesaurus = c.id_thesaurus
                                     AND cg2.private = true
                               )
                         ) x
                         ORDER BY x.sort_norm
                         LIMIT 50;
            """, nativeQuery = true)
    List<Object[]> searchStartWithSynonyms(@Param("val") String val, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
            SELECT idgroup AS id, lexicalvalue AS value
            FROM concept_group_label
            WHERE idthesaurus = :idThesaurus
              AND lang = :idLang
              AND (
                unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT(:value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('% ', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%-', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%(', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%\\_', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%''', :value, '%')))
                OR unaccent(lower(lexicalvalue)) LIKE unaccent(lower(CONCAT('%ʿ', :value, '%')))
              )
            LIMIT 50
            """, nativeQuery = true)
    List<Object[]> searchCollectionsByPrefix(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
            SELECT DISTINCT id_facet AS id, lexical_value AS value
            FROM node_label
            WHERE id_thesaurus = :idThesaurus
              AND lang = :idLang
              AND (
                unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT(:value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('% ', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%-', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%(', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%\\_', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%''', :value, '%')))
                OR unaccent(lower(lexical_value)) LIKE unaccent(lower(CONCAT('%ʿ', :value, '%')))
              )
            LIMIT 50
            """, nativeQuery = true)
    List<Object[]> searchFacetsByPrefix(@Param("value") String value, @Param("idLang") String idLang, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT pt.id_concept, t.lexical_value, t.id_term, c.status
                FROM term t
                JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                JOIN concept c ON pt.id_concept = c.id_concept AND pt.id_thesaurus = c.id_thesaurus
                WHERE t.id_thesaurus = :idThesaurus
                  AND (:idLang IS NULL OR t.lang = :idLang)
                  AND c.status != 'CA'
                  AND (' ' || lower(
                          unaccent(
                              translate(
                                  t.lexical_value,
                                  '[]()~-\''"`.,;:!?',
                                  '                    '
                              )
                          )
                      ) || ' ') LIKE '% ' || lower(unaccent(:value)) || ' %'
                    ORDER BY
                      CASE
                        WHEN unaccent(lower(t.lexical_value)) ILIKE :value THEN 1
                        WHEN unaccent(lower(t.lexical_value)) ILIKE CONCAT(:value, ' %') THEN 2
                      END,
                      unaccent(lower(t.lexical_value))
            """, nativeQuery = true)
    List<Object[]> searchExactPreferredTerms(
            @Param("idThesaurus") String idThesaurus,
            @Param("idLang") String idLang,
            @Param("value") String value
    );

    @Query(value = """
                SELECT preferred_term.id_concept, term.id_term,
                       non_preferred_term.lexical_value AS alt_label,
                       term.lexical_value AS pref_label,
                       concept.status
                FROM non_preferred_term
                JOIN preferred_term ON preferred_term.id_term = non_preferred_term.id_term
                                    AND preferred_term.id_thesaurus = non_preferred_term.id_thesaurus
                JOIN term ON term.id_term = preferred_term.id_term
                          AND term.lang = non_preferred_term.lang
                          AND term.id_thesaurus = preferred_term.id_thesaurus
                JOIN concept ON concept.id_concept = preferred_term.id_concept
                            AND concept.id_thesaurus = preferred_term.id_thesaurus
                WHERE non_preferred_term.id_thesaurus = :thesaurusId
                  AND (' ' || lower(
                          unaccent(
                              translate(
                                  non_preferred_term.lexical_value,
                                  '[]()~-\''"`.,;:!?',
                                  '                    '
                              )
                          )
                      ) || ' ') LIKE '% ' || lower(unaccent(:value)) || ' %'
                  AND non_preferred_term.lang = :lang
                  AND concept.status != 'CA'
                                    ORDER BY
                      CASE
                        WHEN unaccent(lower(non_preferred_term.lexical_value)) ILIKE :value THEN 1
                        WHEN unaccent(lower(non_preferred_term.lexical_value)) ILIKE CONCAT(:value, ' %') THEN 2
                      END,
                      unaccent(lower(non_preferred_term.lexical_value))
                LIMIT 50
            """, nativeQuery = true)
    List<Object[]> searchExactAltTerms(
            @Param("thesaurusId") String thesaurusId,
            @Param("lang") String lang,
            @Param("value") String value
    );

    @Query(value = """
                SELECT preferred_term.id_concept, term.lexical_value, preferred_term.id_term
                FROM preferred_term
                JOIN term ON term.id_term = preferred_term.id_term
                         AND term.id_thesaurus = preferred_term.id_thesaurus
                WHERE term.id_thesaurus = :thesaurusId
                  AND unaccent(lower(term.lexical_value)) = unaccent(lower(:value))
                  AND (:lang IS NULL OR term.lang = :lang)
                ORDER BY term.lexical_value ASC
                LIMIT 200
            """, nativeQuery = true)
    List<Object[]> searchPreferredTermsExact(@Param("value") String value, @Param("lang") String lang, @Param("thesaurusId") String thesaurusId);

    @Query(value = """
                SELECT preferred_term.id_concept, term.id_term,
                       non_preferred_term.lexical_value AS alt_label,
                       term.lexical_value AS pref_label
                FROM non_preferred_term
                JOIN preferred_term ON preferred_term.id_term = non_preferred_term.id_term
                                    AND preferred_term.id_thesaurus = non_preferred_term.id_thesaurus
                JOIN term ON term.id_term = preferred_term.id_term
                          AND term.lang = non_preferred_term.lang
                          AND term.id_thesaurus = preferred_term.id_thesaurus
                WHERE non_preferred_term.id_thesaurus = :thesaurusId
                  AND unaccent(lower(non_preferred_term.lexical_value)) = unaccent(lower(:value))
                  AND (:lang IS NULL OR non_preferred_term.lang = :lang)
                ORDER BY non_preferred_term.lexical_value ASC
                LIMIT 200
            """, nativeQuery = true)
    List<Object[]> searchSynonymsExact(@Param("value") String value, @Param("lang") String lang, @Param("thesaurusId") String thesaurusId);

    @Query(value = """
            SELECT DISTINCT pt.id_concept
            FROM note n
            JOIN preferred_term pt ON pt.id_term = n.id_term AND pt.id_thesaurus = n.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE c.status != 'CA'
              AND n.id_thesaurus = :idThesaurus
              AND (:idLang IS NULL OR n.lang = :idLang)
              AND (
                  (:w1 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w1)
                  AND (:w2 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w2)
                  AND (:w3 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w3)
                  AND (:w4 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w4)
              )
            LIMIT 50
            """, nativeQuery = true)
    List<String> searchConceptIdsFromTermNotes(
            @Param("idThesaurus") String idThesaurus,
            @Param("idLang") String idLang,
            @Param("w1") String w1,
            @Param("w2") String w2,
            @Param("w3") String w3,
            @Param("w4") String w4);

    @Query(value = """
            SELECT DISTINCT pt.id_concept
            FROM note n
            JOIN preferred_term pt ON pt.id_concept = n.id_concept AND pt.id_thesaurus = n.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE c.status != 'CA'
              AND n.id_thesaurus = :idThesaurus
              AND (:idLang IS NULL OR n.lang = :idLang)
              AND (
                  (:w1 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w1)
                  AND (:w2 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w2)
                  AND (:w3 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w3)
                  AND (:w4 IS NULL OR f_unaccent(lower(n.lexicalvalue)) LIKE :w4)
              )
            LIMIT 50
            """, nativeQuery = true)
    List<String> searchConceptIdsFromConceptNotes(
            @Param("idThesaurus") String idThesaurus,
            @Param("idLang") String idLang,
            @Param("w1") String w1,
            @Param("w2") String w2,
            @Param("w3") String w3,
            @Param("w4") String w4);

    @Query(value = """
            SELECT id_concept
            FROM (
                SELECT\s
                    c.id_concept,
                    t.lexical_value,
                    unaccent(lower(t.lexical_value)) = unaccent(lower(:value)) AS is_exact_match
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept AND c.id_thesaurus = cgc.idthesaurus
                WHERE c.id_thesaurus = :idThesaurus
                  AND c.status != 'CA'
                  AND unaccent(lower(t.lexical_value)) LIKE unaccent(lower(:value))
                  AND (:idLang IS NULL OR t.lang = :idLang)
                  AND (:hasGroups = false OR cgc.idgroup IN :idGroups)
            ) AS results
            GROUP BY id_concept, lexical_value, is_exact_match
            ORDER BY CASE WHEN is_exact_match THEN 1 ELSE 2 END, lexical_value
            LIMIT 100
            """, nativeQuery = true)
    List<String> searchPreferredConceptsForAutoCompletion(@Param("value") String value,
                                                          @Param("idLang") String idLang,
                                                          @Param("idThesaurus") String idThesaurus,
                                                          @Param("idGroups") List<String> idGroups,
                                                          @Param("hasGroups") boolean hasGroups);

    @Query(value = """
            SELECT DISTINCT ON (c.id_concept) c.id_concept
            FROM concept c
            JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            JOIN non_preferred_term npt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
            LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept AND c.id_thesaurus = cgc.idthesaurus
            WHERE c.id_thesaurus = :idThesaurus
              AND c.status != 'CA'
              AND unaccent(lower(npt.lexical_value)) LIKE unaccent(lower(:value))
              AND (:idLang IS NULL OR npt.lang = :idLang)
              AND (:hasGroups = false OR cgc.idgroup IN :idGroups)
            ORDER BY c.id_concept,
                CASE WHEN unaccent(lower(npt.lexical_value)) = unaccent(lower(:value)) THEN 1 ELSE 2 END,
                npt.lexical_value
            LIMIT 100
            """, nativeQuery = true)
    List<String> searchAltConceptsForAutoCompletion(@Param("value") String value,
                                                    @Param("idLang") String idLang,
                                                    @Param("idThesaurus") String idThesaurus,
                                                    @Param("idGroups") List<String> idGroups,
                                                    @Param("hasGroups") boolean hasGroups);


    @Query(value = """
            SELECT lower(lexical_value)
            FROM term
            WHERE id_thesaurus = :idThesaurus AND lang = :idLang
            GROUP BY lower(lexical_value)
            HAVING COUNT(*) > 1
            """, nativeQuery = true)
    List<String> findDuplicatePreferredTerms(@Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang);

    @Query(value = """
            SELECT lower(t.lexical_value)
            FROM term t, non_preferred_term npt
            WHERE t.id_thesaurus = npt.id_thesaurus
            AND t.lang = npt.lang
            AND lower(t.lexical_value) = lower(npt.lexical_value)
            AND t.id_thesaurus = :idThesaurus
            AND t.lang = :idLang
            """, nativeQuery = true)
    List<String> findPreferredAndAltLabelDuplicates(@Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang);

    @Query(value = """
            SELECT lower(lexical_value)
            FROM non_preferred_term
            WHERE id_thesaurus = :idThesaurus AND lang = :idLang
            GROUP BY lower(lexical_value)
            HAVING COUNT(*) > 1
            """, nativeQuery = true)
    List<String> findDuplicateAltLabels(@Param("idThesaurus") String idThesaurus, @Param("idLang") String idLang);

    @Query(value = """
                SELECT c.id_concept as idConcept, c.id_ark as idArk, c.id_handle as idHandle,
                       t.lexical_value as lexicalValue, t.lang as lang
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                WHERE c.id_thesaurus = :idThesaurus
                  AND c.status NOT IN ('DEP', 'dep, ''CA')
                  AND (:idLang IS NULL OR t.lang = :idLang)
                  AND (
                    :idGroups IS NULL OR c.id_concept IN (
                        SELECT idconcept FROM concept_group_concept WHERE idgroup IN (:idGroups)
                    )
                  )
                  AND (
                    " +
            ":conditions" +
            "
                  )
                ORDER BY t.lexical_value ASC
                LIMIT 100
            """, nativeQuery = true)
    List<NodeAutoCompletionProjection> searchPreferredTerms(@Param("idThesaurus") String idThesaurus,
                                                            @Param("idLang") String idLang,
                                                            @Param("idGroups") List<String> idGroups,
                                                            @Param("conditions") String conditions);

    @Query(value = """
                SELECT c.id_concept as idConcept, c.id_ark as idArk, c.id_handle as idHandle,
                       npt.lexical_value as lexicalValue, npt.lang as lang
                FROM concept c
                JOIN preferred_term pt ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                JOIN non_preferred_term npt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                WHERE c.id_thesaurus = :idThesaurus
                  AND c.status NOT IN ('DEP', 'dep', 'CA')
                  AND (:idLang IS NULL OR npt.lang = :idLang)
                  AND (:idGroups IS NULL OR c.id_concept IN (SELECT idconcept FROM concept_group_concept WHERE idgroup IN (:idGroups)))
                  AND (" + ":conditions" + ")
                ORDER BY npt.lexical_value ASC
                LIMIT 100
            """, nativeQuery = true)
    List<NodeAutoCompletionProjection> searchSynonymTerms(@Param("idThesaurus") String idThesaurus,
                                                          @Param("idLang") String idLang,
                                                          @Param("idGroups") List<String> idGroups,
                                                          @Param("conditions") String conditions);

    @Query("SELECT new fr.cnrs.opentheso.models.search.NodeSearchMini(cgl.idGroup) "
            + "FROM ConceptGroupLabel cgl WHERE cgl.idThesaurus = :idThesaurus AND cgl.lang = :idLang AND LOWER(cgl.idGroup) = LOWER(:identifier)")
    List<NodeSearchMini> searchCollectionsById(@Param("identifier") String identifier,
                                               @Param("idLang") String idLang,
                                               @Param("idThesaurus") String idThesaurus);

    @Query("SELECT new fr.cnrs.opentheso.models.search.NodeSearchMini(nl.idFacet, nl.lexicalValue) "
            + "FROM NodeLabel nl WHERE nl.idThesaurus = :idThesaurus AND nl.lang = :idLang AND nl.idFacet = :identifier")
    List<NodeSearchMini> searchFacetsById(@Param("identifier") String identifier,
                                          @Param("idLang") String idLang,
                                          @Param("idThesaurus") String idThesaurus);

    @Query("SELECT cgc.idConcept " +
            "FROM ConceptGroupConcept cgc " +
            "WHERE cgc.idThesaurus = :idThesaurus " +
            "GROUP BY cgc.idConcept " +
            "HAVING COUNT(cgc.idConcept) > 1")
    List<String> searchConceptWithMultiGroup(@Param("idThesaurus") String idThesaurus);

    @Query("SELECT c.idConcept FROM Concept c " +
            "WHERE LOWER(c.notation) = LOWER(:notation) " +
            "AND c.idThesaurus = :idThesaurus")
    List<String> searchNotationId(@Param("notation") String notation, @Param("idThesaurus") String idThesaurus);

    @Query("SELECT c.idConcept " +
            "FROM Concept c " +
            "WHERE c.idThesaurus = :idThesaurus " +
            "  AND c.status != 'CA' " +
            "  AND lower(:value) IN (LOWER(c.idConcept), LOWER(c.idArk), LOWER(c.idHandle), LOWER(c.notation))")
    List<String> searchForIds(@Param("value") String value, @Param("idThesaurus") String idThesaurus);

    @Query(value = "SELECT concept.id_concept FROM concept " +
            "WHERE concept.id_thesaurus = :idThesaurus " +
            "AND concept.status != 'CA' " +
            "AND concept.id_concept NOT IN (" +
            "    SELECT idconcept FROM concept_group_concept WHERE idthesaurus = :idThesaurus) " +
            "LIMIT 200",
            nativeQuery = true)
    List<String> searchConceptWithoutGroup(@Param("idThesaurus") String idThesaurus);

    @Query(value = "SELECT id_concept1 FROM hierarchical_relationship " +
            "WHERE role = 'BT' AND id_thesaurus = :idThesaurus " +
            "GROUP BY id_concept1 HAVING COUNT(id_concept1) > 1 LIMIT 200",
            nativeQuery = true)
    List<String> searchAllPolyHierarchy(@Param("idThesaurus") String idThesaurus);

    @Query(value = "SELECT id_concept FROM concept " +
            "WHERE LOWER(status) = LOWER('dep') AND id_thesaurus = :idThesaurus " +
            "LIMIT 200", nativeQuery = true)
    List<String> searchAllDeprecatedConcepts(@Param("idThesaurus") String idThesaurus);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM hierarchical_relationship hr1
                WHERE hr1.id_thesaurus = :idThesaurus
                  AND hr1.id_concept1 = :idConcept
                  AND hr1.role IN ('NT', 'BT')
                  AND hr1.id_concept2 IN (
                      SELECT hr2.id_concept2
                      FROM hierarchical_relationship hr2
                      WHERE hr2.id_thesaurus = :idThesaurus
                        AND hr2.id_concept1 = :idConcept
                        AND hr2.role = 'RT'))
            """, nativeQuery = true)
    boolean isConceptHaveRTandBT(@Param("idConcept") String idConcept, @Param("idThesaurus") String idThesaurus);

    @Query(value = """
                SELECT DISTINCT pt.id_concept AS idConcept, similarity(unaccent(lower(t.lexical_value)), unaccent(lower(:value))) AS score
                FROM term t
                    JOIN preferred_term pt ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
                    JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                    LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept
                    LEFT JOIN concept_group cg ON cgc.idgroup = cg.idgroup
                WHERE c.id_thesaurus = :idThesaurus
                  AND (:idLang IS NULL OR t.lang = :idLang)
                  AND similarity(unaccent(lower(t.lexical_value)), unaccent(lower(:value))) > 0.2
                  AND c.status != 'CA'
                  AND (:isPrivate = false OR cg.private IS NULL OR cg.private = false)
                ORDER BY score DESC
                LIMIT 50;
            """, nativeQuery = true)
    List<ConceptIdOnly> searchPreferredTermsFullTextId(@Param("value") String value, @Param("idLang") String idLang,
                                                       @Param("idThesaurus") String idThesaurus, @Param("isPrivate") boolean isPrivate);


    @Query(value = """
                SELECT DISTINCT pt.id_concept AS idConcept, similarity(unaccent(lower(npt.lexical_value)), unaccent(lower(:value))) AS score
                FROM non_preferred_term npt
                    JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
                    JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus AND npt.lang = t.lang
                    JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
                    LEFT JOIN concept_group_concept cgc ON c.id_concept = cgc.idconcept
                    LEFT JOIN concept_group cg ON cgc.idgroup = cg.idgroup
                WHERE c.id_thesaurus = :idThesaurus
                    AND (:idLang IS NULL OR t.lang = :idLang)
                    AND similarity(unaccent(lower(npt.lexical_value)), unaccent(lower(:value))) > 0.2
                    AND c.status != 'CA'
                    AND (:isPrivate = false OR cg.private IS NULL OR cg.private = false)
                ORDER BY score DESC
                LIMIT 50
            """, nativeQuery = true)
    List<ConceptIdOnly> searchAltTermsFullTextId(@Param("value") String value, @Param("idLang") String idLang,
                                                 @Param("idThesaurus") String idThesaurus,
                                                 @Param("isPrivate") boolean isPrivate);

    // Recherche exacte prefixe sur les termes préférés
    @Query("SELECT new fr.cnrs.opentheso.models.nodes.NodeIdValue(pt.idConcept, t.lexicalValue) " +
            "FROM Term t " +
            "JOIN PreferredTerm pt ON pt.idTerm = t.idTerm AND pt.idThesaurus = t.idThesaurus " +
            "JOIN Concept c ON c.idConcept = pt.idConcept AND c.idThesaurus = pt.idThesaurus " +
            "WHERE t.lang = :lang AND t.idThesaurus = :theso " +
            "AND LOWER(FUNCTION('f_unaccent', t.lexicalValue)) LIKE LOWER(CONCAT(:value, '%')) " +
            "AND c.status != 'CA' " +
            "ORDER BY t.lexicalValue ASC")
    List<NodeIdValue> searchTermsPrefix(@Param("value") String value,
                                        @Param("lang") String lang,
                                        @Param("theso") String theso);

    // Recherche permutée sur les termes préférés
    @Query("SELECT new fr.cnrs.opentheso.models.nodes.NodeIdValue(pt.idConcept, t.lexicalValue) " +
            "FROM Term t " +
            "JOIN PreferredTerm pt ON pt.idTerm = t.idTerm AND pt.idThesaurus = t.idThesaurus " +
            "JOIN Concept c ON c.idConcept = pt.idConcept AND c.idThesaurus = pt.idThesaurus " +
            "WHERE t.lang = :lang AND t.idThesaurus = :theso " +
            "AND LOWER(FUNCTION('f_unaccent', t.lexicalValue)) LIKE LOWER(CONCAT('% ', :value, '%')) " +
            "AND c.status != 'CA' " +
            "ORDER BY t.lexicalValue ASC")
    List<NodeIdValue> searchTermsPermuted(@Param("value") String value,
                                          @Param("lang") String lang,
                                          @Param("theso") String theso);

    // Même chose pour les synonymes
    @Query("SELECT new fr.cnrs.opentheso.models.nodes.NodeIdValue(pt.idConcept, npt.lexicalValue) " +
            "FROM NonPreferredTerm npt " +
            "JOIN PreferredTerm pt ON pt.idTerm = npt.idTerm AND pt.idThesaurus = npt.idThesaurus " +
            "JOIN Concept c ON c.idConcept = pt.idConcept AND c.idThesaurus = pt.idThesaurus " +
            "WHERE npt.lang = :lang AND npt.idThesaurus = :theso " +
            "AND LOWER(FUNCTION('f_unaccent', npt.lexicalValue)) LIKE LOWER(CONCAT(:value, '%')) " +
            "AND c.status != 'CA' " +
            "ORDER BY npt.lexicalValue ASC")
    List<NodeIdValue> searchSynonymsPrefix(@Param("value") String value,
                                           @Param("lang") String lang,
                                           @Param("theso") String theso);

    @Query("SELECT new fr.cnrs.opentheso.models.nodes.NodeIdValue(pt.idConcept, npt.lexicalValue) " +
            "FROM NonPreferredTerm npt " +
            "JOIN PreferredTerm pt ON pt.idTerm = npt.idTerm AND pt.idThesaurus = npt.idThesaurus " +
            "JOIN Concept c ON c.idConcept = pt.idConcept AND c.idThesaurus = pt.idThesaurus " +
            "WHERE npt.lang = :lang AND npt.idThesaurus = :theso " +
            "AND LOWER(FUNCTION('f_unaccent', npt.lexicalValue)) LIKE LOWER(CONCAT('% ', :value, '%')) " +
            "AND c.status != 'CA' " +
            "ORDER BY npt.lexicalValue ASC")
    List<NodeIdValue> searchSynonymsPermuted(@Param("value") String value,
                                             @Param("lang") String lang,
                                             @Param("theso") String theso);

    @Query(value = """
            SELECT DISTINCT pt.id_concept
            FROM preferred_term pt
            JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN concept_group_concept cgc ON pt.id_concept = cgc.idconcept AND pt.id_thesaurus = cgc.idthesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE pt.id_thesaurus = :idThesaurus
              AND t.lang = :idLang
              AND unaccent(lower(t.lexical_value)) LIKE unaccent(lower(CAST(:value AS TEXT)))
              AND c.status != 'CA'
              AND cgc.idgroup IN (:idGroups)
            ORDER BY t.lexical_value
            LIMIT 100
            """, nativeQuery = true)
    List<String> searchPreferredTermInGroupsAsId(@Param("value") String value,
                                                 @Param("idLang") String idLang,
                                                 @Param("idThesaurus") String idThesaurus,
                                                 @Param("idGroups") List<String> idGroups);

    @Query(value = """
            SELECT DISTINCT pt.id_concept
            FROM preferred_term pt
            JOIN term t ON pt.id_term = t.id_term AND pt.id_thesaurus = t.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE pt.id_thesaurus = :idThesaurus
              AND t.lang = :idLang
              AND unaccent(lower(t.lexical_value)) LIKE unaccent(lower(CAST(:value AS TEXT)))
              AND c.status != 'CA'
            ORDER BY t.lexical_value
            LIMIT 100
            """, nativeQuery = true)
    List<String> searchPreferredTermAsId(@Param("value") String value,
                                         @Param("idLang") String idLang,
                                         @Param("idThesaurus") String idThesaurus);


    @Query(value = """
            SELECT DISTINCT pt.id_concept
            FROM non_preferred_term npt
            JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
            JOIN concept_group_concept cgc ON pt.id_concept = cgc.idconcept AND pt.id_thesaurus = cgc.idthesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE pt.id_thesaurus = :idThesaurus
              AND npt.lang = :idLang
              AND unaccent(lower(npt.lexical_value)) LIKE unaccent(lower(CAST(:value AS TEXT)))
              AND c.status != 'CA'
              AND cgc.idgroup IN (:idGroups)
            ORDER BY npt.lexical_value
            LIMIT 100
            """, nativeQuery = true)
    List<String> searchAltTermInGroupsAsId(@Param("value") String value,
                                           @Param("idLang") String idLang,
                                           @Param("idThesaurus") String idThesaurus,
                                           @Param("idGroups") List<String> idGroups);


    @Query(value = """
            SELECT DISTINCT pt.id_concept
            FROM non_preferred_term npt
            JOIN preferred_term pt ON pt.id_term = npt.id_term AND pt.id_thesaurus = npt.id_thesaurus
            JOIN concept c ON c.id_concept = pt.id_concept AND c.id_thesaurus = pt.id_thesaurus
            WHERE pt.id_thesaurus = :idThesaurus
              AND npt.lang = :idLang
              AND unaccent(lower(npt.lexical_value)) LIKE unaccent(lower(CAST(:value AS TEXT)))
              AND c.status != 'CA'
            ORDER BY npt.lexical_value
            LIMIT 100
            """, nativeQuery = true)
    List<String> searchAltTermAsId(@Param("value") String value,
                                   @Param("idLang") String idLang,
                                   @Param("idThesaurus") String idThesaurus);

    /// Anciennes requêtes désactivées par Miled, elles ne fonctionnent pas correctement, les données retournées ne sont pas pertinentes.
    /// Il reste à intégrer la gestion des Collections privées
    @Query(value = """
                SELECT 
                    pt.id_concept AS idConcept,
                    t.id_term AS idTerm,
                    t.lexical_value AS prefLabel,
                    c.status AS status
                FROM term t
                JOIN preferred_term pt 
                    ON pt.id_term = t.id_term
                    AND pt.id_thesaurus = t.id_thesaurus
                JOIN concept c 
                    ON c.id_concept = pt.id_concept
                    AND c.id_thesaurus = pt.id_thesaurus
                WHERE 
                    c.id_thesaurus = :idThesaurus
                    AND c.status != 'CA'
                    AND (t.lexical_value_norm ILIKE '%' || unaccent(lower(:value)) || '%')
                    AND (:langSensitive = false OR t.lang = :idLang)
                ORDER BY 
                    similarity(t.lexical_value_norm, unaccent(lower(:value))) DESC
                LIMIT 50
            """, nativeQuery = true)
    List<NodeSearchMiniProjection> searchPreferredTermsFullText(
            @Param("value") String value,
            @Param("idLang") String idLang,
            @Param("idThesaurus") String idThesaurus,
            @Param("langSensitive") boolean langSensitive
    );

    @Query(value = """
        SELECT
            pt.id_concept AS idConcept,
            t.id_term AS idTerm,
            npt.lexical_value AS altLabelValue,
            t.lexical_value AS prefLabel,
            c.status AS status
        FROM non_preferred_term npt
        JOIN term t
            ON t.id_term = npt.id_term
            AND t.lang = npt.lang
            AND t.id_thesaurus = npt.id_thesaurus
        JOIN preferred_term pt
            ON pt.id_term = t.id_term
            AND pt.id_thesaurus = t.id_thesaurus
        JOIN concept c
            ON c.id_concept = pt.id_concept
            AND c.id_thesaurus = pt.id_thesaurus
        WHERE
            c.id_thesaurus = :idThesaurus AND
            c.status != 'CA'
            AND (npt.lexical_value_norm ILIKE '%' || unaccent(lower(:value)) || '%')
            AND (:langSensitive = false OR npt.lang = :idLang)
        ORDER BY
            similarity(npt.lexical_value_norm, unaccent(lower(:value))) DESC
        LIMIT 50;
            """, nativeQuery = true)
    List<NodeSearchMiniAltProjection> searchAltTermsFullText(
            @Param("value") String value,
            @Param("idLang") String idLang,
            @Param("idThesaurus") String idThesaurus,
            @Param("langSensitive") boolean langSensitive
    );


    @Query(value = "SELECT DISTINCT new fr.cnrs.opentheso.models.search.NodeSearchMini("
            + "pt.idConcept, t.idTerm, t.lexicalValue, c.status) "
            + "FROM Term t "
            + "JOIN PreferredTerm pt ON pt.idTerm = t.idTerm AND pt.idThesaurus = t.idThesaurus "
            + "JOIN Concept c ON c.idConcept = pt.idConcept AND c.idThesaurus = pt.idThesaurus "
            + "LEFT JOIN ConceptGroupConcept cgc ON c.idConcept = cgc.idConcept "
            + "LEFT JOIN ConceptGroup cg ON cgc.idGroup = cg.idGroup "
            + "WHERE t.lang = :idLang AND c.idThesaurus = :idThesaurus "
            + "AND (c.idConcept = :identifier OR c.idArk = :identifier OR c.idHandle = :identifier OR c.notation = :identifier) "
            + "AND c.status != 'CA' "
            + "GROUP BY pt.idConcept, t.idTerm, t.lexicalValue, c.status, cg.isPrivate "
            + "HAVING (cg.isPrivate IS NULL OR cg.isPrivate = false)")
    List<NodeSearchMini> searchConceptByAllIdPrivate(@Param("identifier") String identifier,
                                                     @Param("idLang") String idLang,
                                                     @Param("idThesaurus") String idThesaurus);

    @Query(value = "SELECT new fr.cnrs.opentheso.models.search.NodeSearchMini("
            + "pt.idConcept, t.idTerm, t.lexicalValue, c.status) "
            + "FROM Term t "
            + "JOIN PreferredTerm pt ON pt.idTerm = t.idTerm AND pt.idThesaurus = t.idThesaurus "
            + "JOIN Concept c ON c.idConcept = pt.idConcept AND c.idThesaurus = pt.idThesaurus "
            + "WHERE t.lang = :idLang AND c.idThesaurus = :idThesaurus "
            + "AND (c.idConcept = :identifier OR c.idArk = :identifier OR c.idHandle = :identifier OR c.notation = :identifier) "
            + "AND c.status != 'CA'")
    List<NodeSearchMini> searchConceptByAllIdPublic(@Param("identifier") String identifier, @Param("idLang") String idLang,
                                                    @Param("idThesaurus") String idThesaurus);

}