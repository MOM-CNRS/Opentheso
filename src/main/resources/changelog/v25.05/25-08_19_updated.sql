
DROP FUNCTION IF EXISTS public.opentheso_get_next_nt(character varying, character varying, character varying, integer, integer);

CREATE OR REPLACE FUNCTION public.opentheso_get_next_nt(
	idtheso character varying,
	idconcept character varying,
	idlang character varying,
	offset_ integer,
	step integer,
    isPublic boolean)
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
FOR relation_rec IN SELECT * FROM opentheso_get_nt(idtheso, idconcept, offset_, step, isPublic)
                                      LOOP
    tmpLabel := '';
SELECT libelle INTO tmpLabel
FROM opentheso_get_conceptlabel(idtheso, relation_rec.relationship_id_concept, idlang);

tmp := opentheso_get_uri(
                           theso_rec.original_uri_is_ark,
                           relation_rec.relationship_id_ark,
                           theso_rec.original_uri,
                           theso_rec.original_uri_is_handle,
                           relation_rec.relationship_id_handle,
                           theso_rec.original_uri_is_doi,
                           relation_rec.relationship_id_doi,
                           relation_rec.relationship_id_concept,
                           idtheso,
                           theso_rec.chemin_site)
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

ALTER FUNCTION public.opentheso_get_next_nt(character varying, character varying, character varying, integer, integer, boolean)
    OWNER TO opentheso;




DROP FUNCTION IF EXISTS public.opentheso_get_nt(character varying, character varying, integer, integer, boolean);

CREATE OR REPLACE FUNCTION public.opentheso_get_nt(
	id_theso character varying,
	id_con character varying,
	offset_ integer,
	step integer,
	isprivate boolean)
    RETURNS TABLE(relationship_id_concept character varying, relationship_role character varying, relationship_id_ark character varying, relationship_id_handle character varying, relationship_id_doi character varying)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$
DECLARE
sql_query text;
BEGIN
    sql_query := '
        SELECT DISTINCT hr.id_concept2, hr.role, c.id_ark, c.id_handle,  c.id_doi
        FROM hierarchical_relationship hr
        JOIN concept c ON hr.id_concept2 = c.id_concept AND hr.id_thesaurus = c.id_thesaurus
        JOIN concept_group_concept cgc ON cgc.idconcept = hr.id_concept2
        JOIN concept_group cg ON cgc.idgroup = cg.idgroup
        WHERE hr.id_thesaurus = $1
          AND hr.id_concept1 = $2
          AND ($5 OR (NOT $5 AND cg.private = false) OR (NOT $5 AND cg.private IS NULL))
          AND c.status != ''CA''
          AND hr.role LIKE ''NT%''';

    IF step > 0 THEN
        sql_query := sql_query || format(' OFFSET %s FETCH NEXT %s ROWS ONLY', offset_, step);
END IF;

RETURN QUERY EXECUTE sql_query USING id_theso, id_con, offset_, step, isPrivate;
END;
$BODY$;

ALTER FUNCTION public.opentheso_get_nt(character varying, character varying, integer, integer, boolean)
    OWNER TO opentheso;
