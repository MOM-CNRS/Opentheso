-- Export SKOS/CSV/PDF : réécriture set-based de opentheso_get_concepts
-- Remplace les boucles PL/pgSQL (1 concept × ~10 requêtes) par des agrégations string_agg.

DROP FUNCTION IF EXISTS public.opentheso_get_concepts(character varying, character varying);
DROP FUNCTION IF EXISTS public.opentheso_get_concepts_by_group(character varying, character varying, character varying);

CREATE OR REPLACE FUNCTION public.opentheso_get_concepts(
    id_theso character varying,
    path character varying)
    RETURNS SETOF record
    LANGUAGE plpgsql
    VOLATILE
    PARALLEL UNSAFE
    ROWS 1000
AS $BODY$
DECLARE
    seperateur constant varchar := '##';
    sous_seperateur constant varchar := '@@';
BEGIN
    RETURN QUERY
    WITH
    prefs AS (
        SELECT p.*
        FROM preferences p
        WHERE p.id_thesaurus = id_theso
    ),
    concepts AS (
        SELECT c.*
        FROM concept c
        WHERE c.id_thesaurus = id_theso
          AND c.status <> 'CA'
    ),
    pref_labs AS (
        SELECT pt.id_concept,
               string_agg(t.lexical_value || sous_seperateur || t.lang || seperateur, '' ORDER BY t.lexical_value) AS val
        FROM preferred_term pt
        JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
        WHERE pt.id_thesaurus = id_theso
        GROUP BY pt.id_concept
    ),
    alt_labs AS (
        SELECT pt.id_concept,
               string_agg(npt.lexical_value || sous_seperateur || npt.lang || seperateur, '' ORDER BY npt.lexical_value) AS val
        FROM preferred_term pt
        JOIN non_preferred_term npt
            ON npt.id_term = pt.id_term AND npt.id_thesaurus = pt.id_thesaurus
        WHERE pt.id_thesaurus = id_theso
          AND COALESCE(npt.hiden, false) = false
        GROUP BY pt.id_concept
    ),
    alt_labs_hidden AS (
        SELECT pt.id_concept,
               string_agg(npt.lexical_value || sous_seperateur || npt.lang || seperateur, '' ORDER BY npt.lexical_value) AS val
        FROM preferred_term pt
        JOIN non_preferred_term npt
            ON npt.id_term = pt.id_term AND npt.id_thesaurus = pt.id_thesaurus
        WHERE pt.id_thesaurus = id_theso
          AND COALESCE(npt.hiden, false) = true
        GROUP BY pt.id_concept
    ),
    notes_agg AS (
        SELECT n.identifier AS id_concept,
               string_agg(CASE WHEN n.notetypecode = 'definition' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS definition,
               string_agg(CASE WHEN n.notetypecode = 'example' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS example,
               string_agg(CASE WHEN n.notetypecode = 'editorialNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS editorialNote,
               string_agg(CASE WHEN n.notetypecode = 'changeNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS changeNote,
               string_agg(CASE WHEN n.notetypecode = 'scopeNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS secopeNote,
               string_agg(CASE WHEN n.notetypecode = 'note' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS note,
               string_agg(CASE WHEN n.notetypecode = 'historyNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS historyNote
        FROM note n
        WHERE n.id_thesaurus = id_theso
        GROUP BY n.identifier
    ),
    relations_agg AS (
        SELECT hr.id_concept1 AS id_concept,
               string_agg(
                   CASE WHEN hr.role IN ('NT', 'NTP', 'NTI', 'NTG') THEN
                       opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                         c2.id_handle, pr.original_uri_is_doi, c2.id_doi, hr.id_concept2, id_theso, path)
                       || sous_seperateur || hr.role || sous_seperateur || hr.id_concept2 || sous_seperateur || seperateur
                   END, '') AS narrower,
               string_agg(
                   CASE WHEN hr.role IN ('BT', 'BTP', 'BTI', 'BTG') THEN
                       opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                         c2.id_handle, pr.original_uri_is_doi, c2.id_doi, hr.id_concept2, id_theso, path)
                       || sous_seperateur || hr.role || sous_seperateur || hr.id_concept2 || sous_seperateur || seperateur
                   END, '') AS broader,
               string_agg(
                   CASE WHEN hr.role IN ('RT', 'RHP', 'RPO') THEN
                       opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                         c2.id_handle, pr.original_uri_is_doi, c2.id_doi, hr.id_concept2, id_theso, path)
                       || sous_seperateur || hr.role || sous_seperateur || hr.id_concept2 || sous_seperateur || seperateur
                   END, '') AS related
        FROM hierarchical_relationship hr
        JOIN concept c2 ON c2.id_concept = hr.id_concept2 AND c2.id_thesaurus = hr.id_thesaurus
        CROSS JOIN prefs pr
        WHERE hr.id_thesaurus = id_theso
          AND c2.status <> 'CA'
        GROUP BY hr.id_concept1
    ),
    alignments_agg AS (
        SELECT a.internal_id_concept AS id_concept,
               string_agg(CASE WHEN a.alignement_id_type = 1 THEN a.uri_target || seperateur END, '') AS exactMatch,
               string_agg(CASE WHEN a.alignement_id_type = 2 THEN a.uri_target || seperateur END, '') AS closeMatch,
               string_agg(CASE WHEN a.alignement_id_type = 3 THEN a.uri_target || seperateur END, '') AS broadMatch,
               string_agg(CASE WHEN a.alignement_id_type = 4 THEN a.uri_target || seperateur END, '') AS relatedMatch,
               string_agg(CASE WHEN a.alignement_id_type = 5 THEN a.uri_target || seperateur END, '') AS narrowMatch
        FROM alignement a
        WHERE a.internal_id_thesaurus = id_theso
        GROUP BY a.internal_id_concept
    ),
    gps_agg AS (
        SELECT g.id_concept,
               string_agg(g.latitude::text || sous_seperateur || g.longitude::text || seperateur, '' ORDER BY g.position NULLS LAST) AS gpsData
        FROM gps g
        WHERE g.id_theso = id_theso
        GROUP BY g.id_concept
    ),
    groups_agg AS (
        SELECT cgc.idconcept AS id_concept,
               string_agg(
                   CASE
                       WHEN pr.original_uri_is_ark = true AND cg.id_ark IS NOT NULL AND cg.id_ark <> '' THEN
                           pr.original_uri || '/' || cg.id_ark || seperateur
                       WHEN pr.original_uri_is_ark = true THEN
                           path || '/?idg=' || cg.idgroup || '&idt=' || id_theso || seperateur
                       WHEN cg.id_handle IS NOT NULL AND cg.id_handle <> '' THEN
                           'https://hdl.handle.net/' || cg.id_handle || seperateur
                       WHEN pr.original_uri IS NOT NULL AND pr.original_uri <> '' THEN
                           pr.original_uri || '/?idg=' || cg.idgroup || '&idt=' || id_theso || seperateur
                       ELSE
                           path || '/?idc=' || cg.idgroup || '&idt=' || id_theso || seperateur
                   END, '') AS membre
        FROM concept_group_concept cgc
        JOIN concept_group cg ON cg.idgroup = cgc.idgroup AND cg.idthesaurus = cgc.idthesaurus
        CROSS JOIN prefs pr
        WHERE cgc.idthesaurus = id_theso
        GROUP BY cgc.idconcept
    ),
    images_agg AS (
        SELECT ei.id_concept,
               string_agg(
                   COALESCE(ei.image_name, '') || sous_seperateur
                       || COALESCE(ei.image_copyright, '') || sous_seperateur
                       || COALESCE(ei.external_uri, '') || sous_seperateur
                       || COALESCE(ei.image_creator, '') || seperateur,
                   '') AS img
        FROM external_images ei
        WHERE ei.id_thesaurus = id_theso
        GROUP BY ei.id_concept
    ),
    creators AS (
        SELECT cd.id_concept, MIN(cd.value) AS creator
        FROM concept_dcterms cd
        WHERE cd.id_thesaurus = id_theso
          AND cd.name = 'creator'
        GROUP BY cd.id_concept
    ),
    contributors AS (
        SELECT cd.id_concept, string_agg(cd.value, seperateur) AS contributor
        FROM concept_dcterms cd
        WHERE cd.id_thesaurus = id_theso
          AND cd.name = 'contributor'
        GROUP BY cd.id_concept
    ),
    replaces_agg AS (
        SELECT cr.id_concept2 AS id_concept,
               string_agg(
                   opentheso_get_uri(pr.original_uri_is_ark, c1.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                     c1.id_handle, pr.original_uri_is_doi, c1.id_doi, cr.id_concept1, id_theso, path)
                       || seperateur, '') AS replaces
        FROM concept_replacedby cr
        JOIN concept c1 ON c1.id_concept = cr.id_concept1 AND c1.id_thesaurus = cr.id_thesaurus
        CROSS JOIN prefs pr
        WHERE cr.id_thesaurus = id_theso
        GROUP BY cr.id_concept2
    ),
    replaced_by_agg AS (
        SELECT cr.id_concept1 AS id_concept,
               string_agg(
                   opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                     c2.id_handle, pr.original_uri_is_doi, c2.id_doi, cr.id_concept2, id_theso, path)
                       || seperateur, '') AS replacedBy
        FROM concept_replacedby cr
        JOIN concept c2 ON c2.id_concept = cr.id_concept2 AND c2.id_thesaurus = cr.id_thesaurus
        CROSS JOIN prefs pr
        WHERE cr.id_thesaurus = id_theso
        GROUP BY cr.id_concept1
    ),
    facets_agg AS (
        SELECT ta.id_concept_parent AS id_concept,
               string_agg(ta.id_facet || seperateur, '') AS facets
        FROM thesaurus_array ta
        WHERE ta.id_thesaurus = id_theso
        GROUP BY ta.id_concept_parent
    ),
    external_res_agg AS (
        SELECT er.id_concept,
               string_agg(er.external_uri || seperateur, '') AS externalResources
        FROM external_resources er
        WHERE er.id_thesaurus = id_theso
        GROUP BY er.id_concept
    )
    SELECT
        opentheso_get_uri(pr.original_uri_is_ark, c.id_ark, pr.original_uri, pr.original_uri_is_handle,
                          c.id_handle, pr.original_uri_is_doi, c.id_doi, c.id_concept, id_theso, path)::text,
        c.status::varchar,
        (path || '/?idc=' || c.id_concept || '&idt=' || id_theso)::text,
        c.id_concept::varchar,
        c.id_ark::varchar,
        COALESCE(pl.val, '')::varchar,
        COALESCE(al.val, '')::varchar,
        COALESCE(alh.val, '')::varchar,
        COALESCE(na.definition, '')::text,
        COALESCE(na.example, '')::text,
        COALESCE(na.editorialNote, '')::text,
        COALESCE(na.changeNote, '')::text,
        COALESCE(na.secopeNote, '')::text,
        COALESCE(na.note, '')::text,
        COALESCE(na.historyNote, '')::text,
        c.notation::varchar,
        COALESCE(ra.narrower, '')::text,
        COALESCE(ra.broader, '')::text,
        COALESCE(ra.related, '')::text,
        COALESCE(aa.exactMatch, '')::text,
        COALESCE(aa.closeMatch, '')::text,
        COALESCE(aa.broadMatch, '')::text,
        COALESCE(aa.relatedMatch, '')::text,
        COALESCE(aa.narrowMatch, '')::text,
        COALESCE(ga.gpsData, '')::text,
        COALESCE(grp.membre, '')::text,
        c.created,
        c.modified,
        COALESCE(ia.img, '')::text,
        COALESCE(cr.creator, '')::text,
        COALESCE(co.contributor, '')::text,
        COALESCE(rep.replaces, '')::text,
        COALESCE(rby.replacedBy, '')::text,
        COALESCE(fa.facets, '')::text,
        COALESCE(exa.externalResources, '')::text
    FROM concepts c
    CROSS JOIN prefs pr
    LEFT JOIN pref_labs pl ON pl.id_concept = c.id_concept
    LEFT JOIN alt_labs al ON al.id_concept = c.id_concept
    LEFT JOIN alt_labs_hidden alh ON alh.id_concept = c.id_concept
    LEFT JOIN notes_agg na ON na.id_concept = c.id_concept
    LEFT JOIN relations_agg ra ON ra.id_concept = c.id_concept
    LEFT JOIN alignments_agg aa ON aa.id_concept = c.id_concept
    LEFT JOIN gps_agg ga ON ga.id_concept = c.id_concept
    LEFT JOIN groups_agg grp ON grp.id_concept = c.id_concept
    LEFT JOIN images_agg ia ON ia.id_concept = c.id_concept
    LEFT JOIN creators cr ON cr.id_concept = c.id_concept
    LEFT JOIN contributors co ON co.id_concept = c.id_concept
    LEFT JOIN replaces_agg rep ON rep.id_concept = c.id_concept
    LEFT JOIN replaced_by_agg rby ON rby.id_concept = c.id_concept
    LEFT JOIN facets_agg fa ON fa.id_concept = c.id_concept
    LEFT JOIN external_res_agg exa ON exa.id_concept = c.id_concept;
END;
$BODY$;


CREATE OR REPLACE FUNCTION public.opentheso_get_concepts_by_group(
    id_theso character varying,
    path character varying,
    id_group character varying)
    RETURNS SETOF record
    LANGUAGE plpgsql
    VOLATILE
    PARALLEL UNSAFE
    ROWS 1000
AS $BODY$
DECLARE
    seperateur constant varchar := '##';
    sous_seperateur constant varchar := '@@';
BEGIN
    RETURN QUERY
    WITH
    prefs AS (
        SELECT p.*
        FROM preferences p
        WHERE p.id_thesaurus = id_theso
    ),
    concepts AS (
        SELECT c.*
        FROM concept c
        JOIN concept_group_concept cgc
            ON cgc.idconcept = c.id_concept
           AND cgc.idthesaurus = c.id_thesaurus
        WHERE c.id_thesaurus = id_theso
          AND cgc.idgroup = id_group
          AND c.status <> 'CA'
    ),
    pref_labs AS (
        SELECT pt.id_concept,
               string_agg(t.lexical_value || sous_seperateur || t.lang || seperateur, '' ORDER BY t.lexical_value) AS val
        FROM preferred_term pt
        JOIN term t ON t.id_term = pt.id_term AND t.id_thesaurus = pt.id_thesaurus
        WHERE pt.id_thesaurus = id_theso
          AND pt.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY pt.id_concept
    ),
    alt_labs AS (
        SELECT pt.id_concept,
               string_agg(npt.lexical_value || sous_seperateur || npt.lang || seperateur, '' ORDER BY npt.lexical_value) AS val
        FROM preferred_term pt
        JOIN non_preferred_term npt
            ON npt.id_term = pt.id_term AND npt.id_thesaurus = pt.id_thesaurus
        WHERE pt.id_thesaurus = id_theso
          AND COALESCE(npt.hiden, false) = false
          AND pt.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY pt.id_concept
    ),
    alt_labs_hidden AS (
        SELECT pt.id_concept,
               string_agg(npt.lexical_value || sous_seperateur || npt.lang || seperateur, '' ORDER BY npt.lexical_value) AS val
        FROM preferred_term pt
        JOIN non_preferred_term npt
            ON npt.id_term = pt.id_term AND npt.id_thesaurus = pt.id_thesaurus
        WHERE pt.id_thesaurus = id_theso
          AND COALESCE(npt.hiden, false) = true
          AND pt.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY pt.id_concept
    ),
    notes_agg AS (
        SELECT n.identifier AS id_concept,
               string_agg(CASE WHEN n.notetypecode = 'definition' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS definition,
               string_agg(CASE WHEN n.notetypecode = 'example' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS example,
               string_agg(CASE WHEN n.notetypecode = 'editorialNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS editorialNote,
               string_agg(CASE WHEN n.notetypecode = 'changeNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS changeNote,
               string_agg(CASE WHEN n.notetypecode = 'scopeNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS secopeNote,
               string_agg(CASE WHEN n.notetypecode = 'note' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS note,
               string_agg(CASE WHEN n.notetypecode = 'historyNote' THEN n.lexicalvalue || sous_seperateur || n.lang || seperateur END, '') AS historyNote
        FROM note n
        WHERE n.id_thesaurus = id_theso
          AND n.identifier IN (SELECT id_concept FROM concepts)
        GROUP BY n.identifier
    ),
    relations_agg AS (
        SELECT hr.id_concept1 AS id_concept,
               string_agg(
                   CASE WHEN hr.role IN ('NT', 'NTP', 'NTI', 'NTG') THEN
                       opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                         c2.id_handle, pr.original_uri_is_doi, c2.id_doi, hr.id_concept2, id_theso, path)
                       || sous_seperateur || hr.role || sous_seperateur || hr.id_concept2 || sous_seperateur || seperateur
                   END, '') AS narrower,
               string_agg(
                   CASE WHEN hr.role IN ('BT', 'BTP', 'BTI', 'BTG') THEN
                       opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                         c2.id_handle, pr.original_uri_is_doi, c2.id_doi, hr.id_concept2, id_theso, path)
                       || sous_seperateur || hr.role || sous_seperateur || hr.id_concept2 || sous_seperateur || seperateur
                   END, '') AS broader,
               string_agg(
                   CASE WHEN hr.role IN ('RT', 'RHP', 'RPO') THEN
                       opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                         c2.id_handle, pr.original_uri_is_doi, c2.id_doi, hr.id_concept2, id_theso, path)
                       || sous_seperateur || hr.role || sous_seperateur || hr.id_concept2 || sous_seperateur || seperateur
                   END, '') AS related
        FROM hierarchical_relationship hr
        JOIN concept c2 ON c2.id_concept = hr.id_concept2 AND c2.id_thesaurus = hr.id_thesaurus
        CROSS JOIN prefs pr
        WHERE hr.id_thesaurus = id_theso
          AND c2.status <> 'CA'
          AND hr.id_concept1 IN (SELECT id_concept FROM concepts)
        GROUP BY hr.id_concept1
    ),
    alignments_agg AS (
        SELECT a.internal_id_concept AS id_concept,
               string_agg(CASE WHEN a.alignement_id_type = 1 THEN a.uri_target || seperateur END, '') AS exactMatch,
               string_agg(CASE WHEN a.alignement_id_type = 2 THEN a.uri_target || seperateur END, '') AS closeMatch,
               string_agg(CASE WHEN a.alignement_id_type = 3 THEN a.uri_target || seperateur END, '') AS broadMatch,
               string_agg(CASE WHEN a.alignement_id_type = 4 THEN a.uri_target || seperateur END, '') AS relatedMatch,
               string_agg(CASE WHEN a.alignement_id_type = 5 THEN a.uri_target || seperateur END, '') AS narrowMatch
        FROM alignement a
        WHERE a.internal_id_thesaurus = id_theso
          AND a.internal_id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY a.internal_id_concept
    ),
    gps_agg AS (
        SELECT g.id_concept,
               string_agg(g.latitude::text || sous_seperateur || g.longitude::text || seperateur, '' ORDER BY g.position NULLS LAST) AS gpsData
        FROM gps g
        WHERE g.id_theso = id_theso
          AND g.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY g.id_concept
    ),
    groups_agg AS (
        SELECT cgc.idconcept AS id_concept,
               string_agg(
                   CASE
                       WHEN pr.original_uri_is_ark = true AND cg.id_ark IS NOT NULL AND cg.id_ark <> '' THEN
                           pr.original_uri || '/' || cg.id_ark || seperateur
                       WHEN pr.original_uri_is_ark = true THEN
                           path || '/?idg=' || cg.idgroup || '&idt=' || id_theso || seperateur
                       WHEN cg.id_handle IS NOT NULL AND cg.id_handle <> '' THEN
                           'https://hdl.handle.net/' || cg.id_handle || seperateur
                       WHEN pr.original_uri IS NOT NULL AND pr.original_uri <> '' THEN
                           pr.original_uri || '/?idg=' || cg.idgroup || '&idt=' || id_theso || seperateur
                       ELSE
                           path || '/?idc=' || cg.idgroup || '&idt=' || id_theso || seperateur
                   END, '') AS membre
        FROM concept_group_concept cgc
        JOIN concept_group cg ON cg.idgroup = cgc.idgroup AND cg.idthesaurus = cgc.idthesaurus
        CROSS JOIN prefs pr
        WHERE cgc.idthesaurus = id_theso
          AND cgc.idconcept IN (SELECT id_concept FROM concepts)
        GROUP BY cgc.idconcept
    ),
    images_agg AS (
        SELECT ei.id_concept,
               string_agg(
                   COALESCE(ei.image_name, '') || sous_seperateur
                       || COALESCE(ei.image_copyright, '') || sous_seperateur
                       || COALESCE(ei.external_uri, '') || sous_seperateur
                       || COALESCE(ei.image_creator, '') || seperateur,
                   '') AS img
        FROM external_images ei
        WHERE ei.id_thesaurus = id_theso
          AND ei.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY ei.id_concept
    ),
    creators AS (
        SELECT cd.id_concept, MIN(cd.value) AS creator
        FROM concept_dcterms cd
        WHERE cd.id_thesaurus = id_theso
          AND cd.name = 'creator'
          AND cd.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY cd.id_concept
    ),
    contributors AS (
        SELECT cd.id_concept, string_agg(cd.value, seperateur) AS contributor
        FROM concept_dcterms cd
        WHERE cd.id_thesaurus = id_theso
          AND cd.name = 'contributor'
          AND cd.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY cd.id_concept
    ),
    replaces_agg AS (
        SELECT cr.id_concept2 AS id_concept,
               string_agg(
                   opentheso_get_uri(pr.original_uri_is_ark, c1.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                     c1.id_handle, pr.original_uri_is_doi, c1.id_doi, cr.id_concept1, id_theso, path)
                       || seperateur, '') AS replaces
        FROM concept_replacedby cr
        JOIN concept c1 ON c1.id_concept = cr.id_concept1 AND c1.id_thesaurus = cr.id_thesaurus
        CROSS JOIN prefs pr
        WHERE cr.id_thesaurus = id_theso
          AND cr.id_concept2 IN (SELECT id_concept FROM concepts)
        GROUP BY cr.id_concept2
    ),
    replaced_by_agg AS (
        SELECT cr.id_concept1 AS id_concept,
               string_agg(
                   opentheso_get_uri(pr.original_uri_is_ark, c2.id_ark, pr.original_uri, pr.original_uri_is_handle,
                                     c2.id_handle, pr.original_uri_is_doi, c2.id_doi, cr.id_concept2, id_theso, path)
                       || seperateur, '') AS replacedBy
        FROM concept_replacedby cr
        JOIN concept c2 ON c2.id_concept = cr.id_concept2 AND c2.id_thesaurus = cr.id_thesaurus
        CROSS JOIN prefs pr
        WHERE cr.id_thesaurus = id_theso
          AND cr.id_concept1 IN (SELECT id_concept FROM concepts)
        GROUP BY cr.id_concept1
    ),
    facets_agg AS (
        SELECT ta.id_concept_parent AS id_concept,
               string_agg(ta.id_facet || seperateur, '') AS facets
        FROM thesaurus_array ta
        WHERE ta.id_thesaurus = id_theso
          AND ta.id_concept_parent IN (SELECT id_concept FROM concepts)
        GROUP BY ta.id_concept_parent
    ),
    external_res_agg AS (
        SELECT er.id_concept,
               string_agg(er.external_uri || seperateur, '') AS externalResources
        FROM external_resources er
        WHERE er.id_thesaurus = id_theso
          AND er.id_concept IN (SELECT id_concept FROM concepts)
        GROUP BY er.id_concept
    )
    SELECT
        opentheso_get_uri(pr.original_uri_is_ark, c.id_ark, pr.original_uri, pr.original_uri_is_handle,
                          c.id_handle, pr.original_uri_is_doi, c.id_doi, c.id_concept, id_theso, path)::text,
        c.status::varchar,
        (path || '/?idc=' || c.id_concept || '&idt=' || id_theso)::text,
        c.id_concept::varchar,
        c.id_ark::varchar,
        COALESCE(pl.val, '')::varchar,
        COALESCE(al.val, '')::varchar,
        COALESCE(alh.val, '')::varchar,
        COALESCE(na.definition, '')::text,
        COALESCE(na.example, '')::text,
        COALESCE(na.editorialNote, '')::text,
        COALESCE(na.changeNote, '')::text,
        COALESCE(na.secopeNote, '')::text,
        COALESCE(na.note, '')::text,
        COALESCE(na.historyNote, '')::text,
        c.notation::varchar,
        COALESCE(ra.narrower, '')::text,
        COALESCE(ra.broader, '')::text,
        COALESCE(ra.related, '')::text,
        COALESCE(aa.exactMatch, '')::text,
        COALESCE(aa.closeMatch, '')::text,
        COALESCE(aa.broadMatch, '')::text,
        COALESCE(aa.relatedMatch, '')::text,
        COALESCE(aa.narrowMatch, '')::text,
        COALESCE(ga.gpsData, '')::text,
        COALESCE(grp.membre, '')::text,
        c.created,
        c.modified,
        COALESCE(ia.img, '')::text,
        COALESCE(cr.creator, '')::text,
        COALESCE(co.contributor, '')::text,
        COALESCE(rep.replaces, '')::text,
        COALESCE(rby.replacedBy, '')::text,
        COALESCE(fa.facets, '')::text,
        COALESCE(exa.externalResources, '')::text
    FROM concepts c
    CROSS JOIN prefs pr
    LEFT JOIN pref_labs pl ON pl.id_concept = c.id_concept
    LEFT JOIN alt_labs al ON al.id_concept = c.id_concept
    LEFT JOIN alt_labs_hidden alh ON alh.id_concept = c.id_concept
    LEFT JOIN notes_agg na ON na.id_concept = c.id_concept
    LEFT JOIN relations_agg ra ON ra.id_concept = c.id_concept
    LEFT JOIN alignments_agg aa ON aa.id_concept = c.id_concept
    LEFT JOIN gps_agg ga ON ga.id_concept = c.id_concept
    LEFT JOIN groups_agg grp ON grp.id_concept = c.id_concept
    LEFT JOIN images_agg ia ON ia.id_concept = c.id_concept
    LEFT JOIN creators cr ON cr.id_concept = c.id_concept
    LEFT JOIN contributors co ON co.id_concept = c.id_concept
    LEFT JOIN replaces_agg rep ON rep.id_concept = c.id_concept
    LEFT JOIN replaced_by_agg rby ON rby.id_concept = c.id_concept
    LEFT JOIN facets_agg fa ON fa.id_concept = c.id_concept
    LEFT JOIN external_res_agg exa ON exa.id_concept = c.id_concept;
END;
$BODY$;
