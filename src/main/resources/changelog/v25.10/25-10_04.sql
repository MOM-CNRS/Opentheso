DROP FUNCTION IF EXISTS public.opentheso_get_bt(character varying, character varying, boolean);

CREATE OR REPLACE FUNCTION public.opentheso_get_bt(
	id_theso character varying,
	id_con character varying,
	isprivate boolean)
    RETURNS TABLE(relationship_id_concept character varying, relationship_role character varying, relationship_id_ark character varying, relationship_id_handle character varying, relationship_id_doi character varying)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$

BEGIN
RETURN QUERY

SELECT
    hr.id_concept2,
    hr.role,
    c.id_ark,
    c.id_handle,
    c.id_doi
FROM
    hierarchical_relationship hr
        JOIN
    concept c
    ON hr.id_concept2 = c.id_concept
        AND hr.id_thesaurus = c.id_thesaurus
        LEFT JOIN
    concept_group_concept cgc
    ON cgc.idconcept = hr.id_concept2
        AND cgc.idthesaurus = hr.id_thesaurus
        LEFT JOIN
    concept_group cg
    ON cg.idgroup = cgc.idgroup
        AND cg.idthesaurus = cgc.idthesaurus
WHERE
    hr.id_thesaurus = $1
  AND hr.id_concept1 = $2
  AND (
    isPrivate
        OR (NOT isPrivate AND cg.private = false)
        OR (NOT isPrivate AND cg.private IS NULL)
    )
  AND c.status != 'CA'
    AND hr.role LIKE 'BT%';
END;
$BODY$;