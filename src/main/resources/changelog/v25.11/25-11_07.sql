DROP FUNCTION IF EXISTS public.opentheso_get_rt(character varying, character varying, boolean);
CREATE OR REPLACE FUNCTION public.opentheso_get_rt(
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
        SELECT id_concept2, role, id_ark, id_handle, id_doi
        FROM hierarchical_relationship, concept
        WHERE hierarchical_relationship.id_concept2 = concept.id_concept
          AND hierarchical_relationship.id_thesaurus = concept.id_thesaurus
          AND hierarchical_relationship.id_thesaurus = $1
          AND hierarchical_relationship.id_concept1 = $2
          AND concept.status != 'CA'
          AND role LIKE 'RT%';
END;
$BODY$;