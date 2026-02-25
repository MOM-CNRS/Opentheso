CREATE OR REPLACE FUNCTION public.opentheso_get_nt(
    id_theso character varying,
    id_con character varying,
    offset_ integer,
    step integer,
    isprivate boolean,
    idlang character varying)
RETURNS TABLE(
    relationship_lexical_value character varying,
    relationship_id_concept character varying,
    relationship_role character varying,
    relationship_id_ark character varying,
    relationship_id_handle character varying,
    relationship_id_doi character varying,
    relationship_sort_key integer
)
LANGUAGE plpgsql
AS $BODY$
DECLARE
sql_query text;
BEGIN

    IF isprivate THEN
        sql_query := '
            SELECT DISTINCT term.lexical_value, id_concept2, role, id_ark, id_handle, id_doi,
                CASE
                    WHEN term.lexical_value ~ ''^[0-9]+$'' THEN term.lexical_value::int
                    ELSE NULL
                END AS sort_key
            FROM hierarchical_relationship, concept con, preferred_term pt, term
            WHERE hierarchical_relationship.id_concept2 = con.id_concept
            AND hierarchical_relationship.id_thesaurus = con.id_thesaurus
            AND pt.id_concept = con.id_concept
            AND pt.id_thesaurus = con.id_thesaurus
            AND term.lang = $3
            AND pt.id_term = term.id_term
            AND hierarchical_relationship.id_thesaurus = $1
            AND hierarchical_relationship.id_concept1 = $2
            AND con.status != ''CA''
            AND role LIKE ''NT%''
            ORDER BY sort_key, term.lexical_value ASC';
ELSE
        sql_query := '
            SELECT DISTINCT term.lexical_value, id_concept2, role, con.id_ark, con.id_handle, con.id_doi,
                CASE
                    WHEN term.lexical_value ~ ''^[0-9]+$'' THEN term.lexical_value::int
                    ELSE NULL
                END AS sort_key
            FROM hierarchical_relationship, preferred_term pt, term, concept con
                LEFT JOIN concept_group_concept cgc ON cgc.idconcept = con.id_concept AND cgc.idthesaurus = con.id_thesaurus
                LEFT JOIN concept_group cg ON cg.idgroup = cgc.idgroup
            WHERE hierarchical_relationship.id_concept2 = con.id_concept
            AND hierarchical_relationship.id_thesaurus = con.id_thesaurus
            AND pt.id_concept = con.id_concept
            AND pt.id_thesaurus = con.id_thesaurus
            AND term.lang = $3
            AND pt.id_term = term.id_term
            AND hierarchical_relationship.id_thesaurus = $1
            AND hierarchical_relationship.id_concept1 = $2
            AND con.status != ''CA''
            AND role LIKE ''NT%''
            AND (cg.private IS NULL OR cg.private = false)
            ORDER BY sort_key, term.lexical_value ASC';
    END IF;

    -- Ajout pagination
    IF step > 0 THEN
        sql_query := sql_query || format(' OFFSET %s FETCH NEXT %s ROWS ONLY', offset_, step);
    END IF;

    -- Exécution
    RETURN QUERY EXECUTE sql_query USING id_theso, id_con, idlang;

END;
$BODY$;

ALTER FUNCTION public.opentheso_get_nt(character varying, character varying, integer, integer, boolean, character varying)
    OWNER TO opentheso;




DROP FUNCTION IF EXISTS public.opentheso_get_next_nt(character varying, character varying, character varying, integer, integer, boolean);

CREATE OR REPLACE FUNCTION public.opentheso_get_next_nt(
	idtheso character varying,
	idconcept character varying,
	idlang character varying,
	offset_ integer,
	step integer,
	ispublic boolean)
    RETURNS TABLE(narrower text)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$
DECLARE
seperateur constant varchar := '##';
    sous_seperateur constant varchar := '@@';
    theso_rec record;
    relation_rec record;
    narrower_text text := '';
    tmpLabel text;
    tmp text;

BEGIN
SELECT * INTO theso_rec FROM preferences where id_thesaurus = idtheso;

-- Relations
narrower_text = '';
FOR relation_rec IN SELECT * FROM opentheso_get_nt(idtheso, idconcept, offset_, step, isPublic, idlang)
                                      LOOP
    tmpLabel := '';
SELECT libelle INTO tmpLabel
FROM opentheso_get_conceptlabel(idtheso, relation_rec.relationship_id_concept, idlang);

tmp := opentheso_get_uri(theso_rec.original_uri_is_ark, relation_rec.relationship_id_ark, theso_rec.original_uri,
            theso_rec.original_uri_is_handle, relation_rec.relationship_id_handle, theso_rec.original_uri_is_doi,
            relation_rec.relationship_id_doi, relation_rec.relationship_id_concept, idtheso, theso_rec.chemin_site)
                       || sous_seperateur
                       || relation_rec.relationship_role
                       || sous_seperateur
                       || relation_rec.relationship_id_concept
                       || sous_seperateur
                       || tmpLabel;

            narrower_text := narrower_text || tmp || seperateur;
END LOOP;
return query
SELECT narrower_text;
END;
$BODY$;




DROP FUNCTION IF EXISTS public.opentheso_get_concept(character varying, character varying, character varying, integer, integer, boolean);

CREATE OR REPLACE FUNCTION public.opentheso_get_concept(
	idtheso character varying,
	idconcept character varying,
	idlang character varying,
	offset_ integer,
	step integer,
	isprivate boolean)
    RETURNS SETOF record
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$
DECLARE

seperateur constant varchar := '##';
    sous_seperateur constant varchar := '@@';

    rec record;
    con record;
    theso_rec record;
    traduction_rec record;
    altlab_rec record;
    altlab_hiden_rec record;
    geo_rec record;
    group_rec record;
    note_concept_rec record;
    note_term_rec record;
    relation_rec record;
    alignement_rec record;
    img_rec record;
    vote_record record;
    message_record record;
    cadidat_record record;
    replace_rec record;
    replacedBy_rec record;
    facet_rec record;
    externalResource_rec record;
    contributor_rec record;
    preflab_selected_rec record;
    altlab_selected_rec record;
    altlab_hiden_selected_rec record;

    tmp text;
    uri text;
    local_URI text;

    prefLab_selected VARCHAR;
    altLab_selected VARCHAR;
    altLab_hiden_selected VARCHAR;

    permaLinkId VARCHAR;

    prefLab VARCHAR;
    altLab VARCHAR;
    altLab_hiden VARCHAR;
    membre text;
    definition text;
    secopeNote text;
    note text;
    historyNote text;
    example text;
    changeNote text;
    editorialNote text;
    narrower text;
    broader text;
    related text;
    exactMatch text;
    closeMatch text;
    broadMatch text;
    relatedMatch text;
    narrowMatch text;
    img text;
    creator text;
    contributor text;
    replaces text;
    replacedBy text;
    facets text;
    externalResources text;
    gpsData text;
    tmpLabel text;

BEGIN
SELECT * INTO con FROM concept WHERE id_thesaurus = idtheso AND id_concept = idconcept AND status != 'CA';
-- Vérifiez si aucun résultat n'a été trouvé
IF con IS NULL THEN
        RETURN QUERY
SELECT NULL::text AS URI,
    NULL::varchar AS resourceType,
    NULL::text AS localUri,
    NULL::varchar AS identifier,
    NULL::varchar AS permalinkId,
    NULL::varchar AS prefLabel,
    NULL::varchar AS altLabel,
    NULL::varchar AS hidenlabel,
    NULL::varchar AS prefLabel_trad,
    NULL::varchar AS altLabel_trad,
    NULL::varchar AS hiddenLabel_trad,
    NULL::text AS definition,
    NULL::text AS example,
    NULL::text AS editorialNote,
    NULL::text AS changeNote,
    NULL::text AS scopeNote,
    NULL::text AS note,
    NULL::text AS historyNote,
    NULL::varchar AS notation,
    NULL::text AS narrower,
    NULL::text AS broader,
    NULL::text AS related,
    NULL::text AS exactMatch,
    NULL::text AS closeMatch,
    NULL::text AS broadMatch,
    NULL::text AS relatedMatch,
    NULL::text AS narrowMatch,
    NULL::text AS gpsData,
    NULL::text AS membre,
    NULL::date AS created,
    NULL::date AS modified,
    NULL::text AS images,
    NULL::text AS creator,
    NULL::text AS contributor,
    NULL::text AS replaces,
    NULL::text AS replaced_by,
    NULL::text AS facets,
    NULL::text AS externalResources,
    NULL::text AS conceptType
    WHERE FALSE;  -- Aucun enregistrement ne sera retourné
RETURN;
END IF;

SELECT * INTO theso_rec FROM preferences where id_thesaurus = idtheso;

-- URI
uri = opentheso_get_uri(theso_rec.original_uri_is_ark, con.id_ark, theso_rec.original_uri, theso_rec.original_uri_is_handle,
                            con.id_handle, theso_rec.original_uri_is_doi, con.id_doi, idconcept, idtheso, theso_rec.chemin_site);

    -- LocalUri
    local_URI = theso_rec.chemin_site || '?idc=' || idconcept || '&idt=' || idtheso;

    permaLinkId = con.id_ark;

    IF(theso_rec.original_uri_is_handle) THEN permaLinkId = con.id_handle;
END IF;

    -- prefLab_selected
    preflab_selected = '';
SELECT * INTO preflab_selected_rec FROM opentheso_get_conceptlabel(idtheso, idconcept, idlang);
preflab_selected = preflab_selected_rec.libelle || sous_seperateur || preflab_selected_rec.idterm || sous_seperateur || preflab_selected_rec.unique_id;

    -- altLab_selected
    altlab_selected = '';
FOR altlab_selected_rec IN SELECT * FROM opentheso_get_altlabel(idtheso, idconcept, idlang, false)
                                             LOOP
    altlab_selected = altlab_selected || altLab_selected_rec.altlabel || sous_seperateur || altLab_selected_rec.idterm || sous_seperateur || altLab_selected_rec.unique_id || seperateur;
END LOOP;

    -- altLab_hiden_selected
    altlab_hiden_selected = '';
FOR altlab_selected_rec IN SELECT * FROM opentheso_get_altLabel(idtheso, idconcept, idlang, true)
                                             LOOP
    altlab_hiden_selected = altlab_hiden_selected || altlab_selected_rec.altlabel || sous_seperateur || altLab_selected_rec.idterm || sous_seperateur || altlab_selected_rec.unique_id || seperateur;
END LOOP;

    -- PrefLabTraduction
    preflab = '';
FOR traduction_rec IN SELECT * FROM opentheso_get_preflabel_traductions(idtheso, idconcept, idlang)
                                        LOOP
    preflab = preflab || traduction_rec.term_id || sous_seperateur || traduction_rec.term_lexical_value || sous_seperateur || traduction_rec.term_lang || sous_seperateur || traduction_rec.unique_id || sous_seperateur || traduction_rec.flag_code || seperateur;
END LOOP;

    -- altLabTraduction
    altLab = '';
FOR altlab_rec IN SELECT * FROM opentheso_get_altLabel_traductions(idtheso, idconcept, idlang, false)
                                    LOOP
    altlab = altlab || altlab_rec.altlabel_id || sous_seperateur || altlab_rec.altlabel || sous_seperateur || altlab_rec.idlang_alt || sous_seperateur || altlab_rec.unique_id || seperateur;
END LOOP;

    -- altLab hiden
    altlab_hiden = '';
FOR altlab_hiden_rec IN SELECT * FROM opentheso_get_altLabel_traductions(idtheso, idconcept, idlang, true)
                                          LOOP
    altlab_hiden = altlab_hiden || altlab_hiden_rec.altlabel_id || sous_seperateur || altlab_hiden_rec.altlabel || sous_seperateur || altlab_hiden_rec.idlang_alt || sous_seperateur || altlab_hiden_rec.unique_id || seperateur;
END LOOP;

    -- Notes
    note = '';
    example = '';
    changeNote = '';
    secopeNote = '';
    definition = '';
    historyNote = '';
    editorialNote = '';
FOR note_concept_rec IN SELECT * FROM opentheso_get_notes(idtheso, idconcept)
                                          LOOP
    IF (note_concept_rec.note_notetypecode = 'note') THEN
                note = note || note_concept_rec.note_id || sous_seperateur || note_concept_rec.note_lexicalvalue || sous_seperateur || note_concept_rec.note_lang || sous_seperateur || COALESCE(note_concept_rec.note_source, '') || seperateur;
ELSIF (note_concept_rec.note_notetypecode = 'scopeNote') THEN
                secopeNote = secopeNote || note_concept_rec.note_id || sous_seperateur || note_concept_rec.note_lexicalvalue || sous_seperateur || note_concept_rec.note_lang || sous_seperateur || COALESCE(note_concept_rec.note_source, '') || seperateur;
            ELSIF (note_concept_rec.note_notetypecode = 'historyNote') THEN
                historyNote = historyNote || note_concept_rec.note_id || sous_seperateur || note_concept_rec.note_lexicalvalue || sous_seperateur || note_concept_rec.note_lang || sous_seperateur || COALESCE(note_concept_rec.note_source, '') || seperateur;
            ELSIF (note_concept_rec.note_notetypecode = 'definition') THEN
                definition = definition || note_concept_rec.note_id || sous_seperateur || note_concept_rec.note_lexicalvalue || sous_seperateur || note_concept_rec.note_lang || sous_seperateur || COALESCE(note_concept_rec.note_source, '') || seperateur;
            ELSIF (note_concept_rec.note_notetypecode = 'example') THEN
                example = example || note_concept_rec.note_id || sous_seperateur || note_concept_rec.note_lexicalvalue || sous_seperateur || note_concept_rec.note_lang || sous_seperateur || COALESCE(note_concept_rec.note_source, '') || seperateur;
            ELSIF (note_concept_rec.note_notetypecode = 'changeNote') THEN
                changeNote = changeNote || note_concept_rec.note_id || sous_seperateur || note_concept_rec.note_lexicalvalue || sous_seperateur || note_concept_rec.note_lang || sous_seperateur || COALESCE(note_concept_rec.note_source, '') || seperateur;
            ELSIF (note_concept_rec.note_notetypecode = 'editorialNote') THEN
                editorialNote = editorialNote || note_concept_rec.note_id || sous_seperateur || note_concept_rec.note_lexicalvalue || sous_seperateur || note_concept_rec.note_lang || sous_seperateur || COALESCE(note_concept_rec.note_source, '') || seperateur;
END IF;
END LOOP;

    -- Relations
    broader = '';
FOR relation_rec IN SELECT * FROM opentheso_get_bt(idtheso, idconcept, isPrivate)
                                      LOOP
    tmpLabel = '';
select libelle INTO tmpLabel from opentheso_get_conceptlabel(idtheso, relation_rec.relationship_id_concept, idLang);
tmp = opentheso_get_uri(theso_rec.original_uri_is_ark, relation_rec.relationship_id_ark, theso_rec.original_uri,
                                    theso_rec.original_uri_is_handle, relation_rec.relationship_id_handle, theso_rec.original_uri_is_doi,
                                    relation_rec.relationship_id_doi, relation_rec.relationship_id_concept, idtheso, theso_rec.chemin_site)
                      || sous_seperateur || relation_rec.relationship_role || sous_seperateur || relation_rec.relationship_id_concept || sous_seperateur || COALESCE(tmpLabel, '') ;
            broader = broader || tmp || seperateur;
END LOOP;

    related = '';
FOR relation_rec IN SELECT * FROM opentheso_get_rt(idtheso, idconcept, isPrivate)
                                      LOOP
    tmpLabel = '';
select libelle INTO tmpLabel from opentheso_get_conceptlabel(idtheso, relation_rec.relationship_id_concept, idLang);
tmp = opentheso_get_uri(theso_rec.original_uri_is_ark, relation_rec.relationship_id_ark, theso_rec.original_uri,
                                    theso_rec.original_uri_is_handle, relation_rec.relationship_id_handle, theso_rec.original_uri_is_doi,
                                    relation_rec.relationship_id_doi, relation_rec.relationship_id_concept, idtheso, theso_rec.chemin_site)
                      || sous_seperateur || relation_rec.relationship_role || sous_seperateur || relation_rec.relationship_id_concept || sous_seperateur || COALESCE(tmpLabel, '') ;
            related = related || tmp || seperateur;
END LOOP;

    narrower = '';
FOR relation_rec IN SELECT * FROM opentheso_get_nt(idtheso, idconcept, offset_, step, isPrivate, idlang)
                                      LOOP
    tmpLabel = '';
select libelle INTO tmpLabel from opentheso_get_conceptlabel(idtheso, relation_rec.relationship_id_concept, idLang);
tmp = opentheso_get_uri(theso_rec.original_uri_is_ark, relation_rec.relationship_id_ark, theso_rec.original_uri,
                                    theso_rec.original_uri_is_handle, relation_rec.relationship_id_handle, theso_rec.original_uri_is_doi,
                                    relation_rec.relationship_id_doi, relation_rec.relationship_id_concept, idtheso, theso_rec.chemin_site)
                      || sous_seperateur || relation_rec.relationship_role || sous_seperateur || relation_rec.relationship_id_concept || sous_seperateur || COALESCE(tmpLabel, '') ;
            narrower = narrower || tmp || seperateur;
END LOOP;

    -- Alignement
    exactMatch = '';
    closeMatch = '';
    broadMatch = '';
    relatedMatch = '';
    narrowMatch = '';
FOR alignement_rec IN SELECT * FROM opentheso_get_alignements(idtheso, idconcept)
                                        LOOP
    if (alignement_rec.alig_id_type = 1) THEN
                exactMatch = exactMatch || alignement_rec.alig_uri_target  || sous_seperateur || idconcept || sous_seperateur || COALESCE(alignement_rec.alig_thesaurus_target, '') || seperateur;
ELSEIF (alignement_rec.alig_id_type = 2) THEN
                closeMatch = closeMatch || alignement_rec.alig_uri_target || sous_seperateur || idconcept || sous_seperateur || COALESCE(alignement_rec.alig_thesaurus_target, '') || seperateur;
            ELSEIF (alignement_rec.alig_id_type = 3) THEN
                broadMatch = broadMatch || alignement_rec.alig_uri_target || sous_seperateur || idconcept || sous_seperateur || COALESCE(alignement_rec.alig_thesaurus_target, '') || seperateur;
            ELSEIF (alignement_rec.alig_id_type = 4) THEN
                relatedMatch = relatedMatch || alignement_rec.alig_uri_target || sous_seperateur || idconcept || sous_seperateur || COALESCE(alignement_rec.alig_thesaurus_target, '') || seperateur;
            ELSEIF (alignement_rec.alig_id_type = 5) THEN
                narrowMatch = narrowMatch || alignement_rec.alig_uri_target || sous_seperateur || idconcept || sous_seperateur || COALESCE(alignement_rec.alig_thesaurus_target, '') || seperateur;
END IF;
END LOOP;

    -- geo:alt && geo:long
    gpsData = '';
FOR geo_rec IN SELECT * FROM opentheso_get_gps(idtheso, idconcept)
                                 LOOP
    gpsData = gpsData || geo_rec.gps_latitude || sous_seperateur || geo_rec.gps_longitude || sous_seperateur || COALESCE(geo_rec.pos, 0) || seperateur;
END LOOP;

    -- membre
    membre = '';
FOR group_rec IN SELECT * FROM opentheso_get_groups(idtheso, idconcept)
                                   LOOP
    tmpLabel = '';
select libelle INTO tmpLabel from opentheso_get_grouplabel(idtheso, group_rec.group_id, idLang);

IF (theso_rec.original_uri_is_ark = true AND group_rec.group_id_ark IS NOT NULL  AND group_rec.group_id_ark != '') THEN
                membre = membre || theso_rec.original_uri || '/' || group_rec.group_id_ark || sous_seperateur || group_rec.group_id || sous_seperateur || COALESCE(tmpLabel, '') || seperateur;
            ELSIF (theso_rec.original_uri_is_ark = true AND (group_rec.group_id_ark IS NULL OR group_rec.group_id_ark = '')) THEN
                membre = membre || theso_rec.chemin_site || '?idg=' || group_rec.group_id || '&idt=' || idtheso || sous_seperateur || group_rec.group_id || sous_seperateur || COALESCE(tmpLabel, '') || seperateur;
            ELSIF (group_rec.group_id_handle IS NOT NULL AND group_rec.group_id_handle != '') THEN
                membre = membre || 'https://hdl.handle.net/' || group_rec.group_id_handle || sous_seperateur || group_rec.group_id || sous_seperateur || COALESCE(tmpLabel, '') || seperateur;
            ELSIF (theso_rec.original_uri IS NOT NULL AND theso_rec.original_uri != '') THEN
                membre = membre || theso_rec.original_uri || '/?idg=' || group_rec.group_id || '&idt=' || idtheso || sous_seperateur || group_rec.group_id || sous_seperateur || COALESCE(tmpLabel, '') || seperateur;
ELSE
                membre = membre || theso_rec.chemin_site || '?idc=' || group_rec.group_id || '&idt=' || idtheso || sous_seperateur || group_rec.group_id || sous_seperateur || COALESCE(tmpLabel, '') || seperateur;
END IF;
END LOOP;

    -- Images
    img = '';
FOR img_rec IN SELECT * FROM opentheso_get_images(idtheso, idconcept)
                                 LOOP
    img = img || img_rec.name || sous_seperateur || img_rec.copyright || sous_seperateur || img_rec.url || sous_seperateur || COALESCE(img_rec.creator, '') || seperateur;
END LOOP;

    -- Agent DcTerms
    creator = '';
    contributor = '';
SELECT value INTO creator FROM concept_dcterms WHERE id_concept = con.id_concept and id_thesaurus = con.id_thesaurus and name = 'creator';

FOR contributor_rec IN SELECT value
                       FROM concept_dcterms
                       WHERE id_concept = con.id_concept
                         AND id_thesaurus = con.id_thesaurus
                         AND name = 'contributor'
                           LOOP
            IF (contributor != '') THEN
                contributor = contributor || seperateur;
END IF;
            contributor = contributor || contributor_rec.value;
END LOOP;

    replaces = '';
FOR replace_rec IN SELECT id_concept1, id_ark, id_handle, id_doi
                   FROM concept_replacedby, concept
                   WHERE concept.id_concept = concept_replacedby.id_concept1
                     AND concept.id_thesaurus = concept_replacedby.id_thesaurus
                     AND concept_replacedby.id_concept2 = idconcept
                     AND concept_replacedby.id_thesaurus = idtheso
                       LOOP
            tmpLabel = '';
select libelle INTO tmpLabel from opentheso_get_conceptlabel(idtheso, replace_rec.id_concept1, idLang);
replaces = replaces || opentheso_get_uri(theso_rec.original_uri_is_ark, replace_rec.id_ark, theso_rec.original_uri,
                                                     theso_rec.original_uri_is_handle, replace_rec.id_handle, theso_rec.original_uri_is_doi,
                                                     replace_rec.id_doi, replace_rec.id_concept1, idtheso, theso_rec.chemin_site) || sous_seperateur || replace_rec.id_concept1 || sous_seperateur || tmpLabel || seperateur;
END LOOP;

    replacedBy = '';
FOR replacedBy_rec IN SELECT id_concept2, id_ark, id_handle, id_doi
                      FROM concept_replacedby, concept
                      WHERE concept.id_concept = concept_replacedby.id_concept2
                        AND concept.id_thesaurus = concept_replacedby.id_thesaurus
                        AND concept_replacedby.id_concept1 = idconcept
                        AND concept_replacedby.id_thesaurus = idtheso
                          LOOP
            tmpLabel = '';
select libelle INTO tmpLabel from opentheso_get_conceptlabel(idtheso, replacedBy_rec.id_concept2, idLang);
replacedBy = replacedBy || opentheso_get_uri(theso_rec.original_uri_is_ark, replacedBy_rec.id_ark, theso_rec.original_uri,
                                                         theso_rec.original_uri_is_handle, replacedBy_rec.id_handle, theso_rec.original_uri_is_doi,
                                                         replacedBy_rec.id_doi, replacedBy_rec.id_concept2, idtheso, theso_rec.chemin_site) || sous_seperateur || replacedBy_rec.id_concept2 || sous_seperateur || tmpLabel || seperateur;
END LOOP;

    facets = '';
FOR facet_rec IN SELECT thesaurus_array.id_facet
                 FROM thesaurus_array
                 WHERE thesaurus_array.id_thesaurus = idtheso
                   AND thesaurus_array.id_concept_parent = idconcept
                     LOOP
            tmpLabel = '';
select libelle INTO tmpLabel from opentheso_get_labelfacet(idtheso, facet_rec.id_facet, idLang);
facets = facets || facet_rec.id_facet || sous_seperateur || tmpLabel || seperateur;
END LOOP;

    externalResources = '';
FOR externalResource_rec IN SELECT external_resources.external_uri, external_resources.id_concept, external_resources.description
                            FROM external_resources
                            WHERE external_resources.id_thesaurus = idtheso
                              AND external_resources.id_concept = idconcept
                                LOOP
            externalResources = externalResources || externalResource_rec.external_uri || sous_seperateur || externalResource_rec.id_concept || sous_seperateur || COALESCE(externalResource_rec.description, '') || seperateur;
END LOOP;

return query
SELECT 	uri, con.status, local_URI, idconcept, permaLinkId, prefLab_selected, altLab_selected, altLab_hiden_selected, prefLab, altLab, altLab_hiden, definition, example,
          editorialNote, changeNote, secopeNote, note, historyNote, con.notation, narrower, broader, related, exactMatch, closeMatch,
          broadMatch, relatedMatch, narrowMatch, gpsData, membre, con.created::date, con.modified::date,
    img, creator, contributor, replaces, replacedBy, facets, externalResources, con.concept_type;

END;
$BODY$;

ALTER FUNCTION public.opentheso_get_concept(character varying, character varying, character varying, integer, integer, boolean)
    OWNER TO opentheso;

