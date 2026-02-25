CREATE OR REPLACE FUNCTION opentheso_get_concept_son(
    id_theso character varying,
    id_bt character varying,
    private boolean
)
    RETURNS TABLE(notation character varying, status character varying, idconcept2 character varying)
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF private THEN
        RETURN QUERY
SELECT concept.notation,
       concept.status,
       hierarchical_relationship.id_concept2
FROM hierarchical_relationship,
     concept
WHERE concept.id_thesaurus = hierarchical_relationship.id_thesaurus
  AND concept.id_concept = hierarchical_relationship.id_concept2
  AND hierarchical_relationship.id_thesaurus = id_theso
  AND id_concept1 = id_bt
  AND role LIKE 'NT%'
  AND concept.status != 'CA'
              AND id_concept2 NOT IN (
                SELECT id_concept
                FROM concept_facet, thesaurus_array
                WHERE concept_facet.id_facet = thesaurus_array.id_facet
                  AND concept_facet.id_thesaurus = thesaurus_array.id_thesaurus
                  AND thesaurus_array.id_concept_parent = id_bt
                  AND thesaurus_array.id_thesaurus = id_theso
            )
ORDER BY concept.notation ASC;
ELSE
        RETURN QUERY
SELECT concept.notation,
       concept.status,
       hr.id_concept2
FROM hierarchical_relationship hr
         JOIN concept ON concept.id_thesaurus = hr.id_thesaurus
    AND concept.id_concept = hr.id_concept2
         LEFT JOIN concept_group_concept cgc ON concept.id_concept = cgc.idconcept
         LEFT JOIN concept_group cg ON cgc.idgroup = cg.idgroup
WHERE hr.id_thesaurus = id_theso
  AND hr.id_concept1 = id_bt
  AND hr.role LIKE 'NT%'
  AND concept.status != 'CA'
GROUP BY concept.notation, concept.status, hr.id_concept2,
    concept.id_ark, concept.id_handle, concept.id_doi
HAVING BOOL_OR(cg.private IS NULL OR cg.private = false)
ORDER BY concept.notation ASC;
END IF;
END;
$$;

alter function opentheso_get_concept_son(varchar, varchar, boolean) owner to opentheso;



DROP function opentheso_get_list_narrower_fortree;

create function opentheso_get_list_narrower_fortree(idtheso character varying, idbt character varying, idlang character varying, private boolean) returns SETOF record
    language plpgsql
as
$$
DECLARE
rec record;
    con record;
    preflab_rec record;

    havechildren boolean;
    prefLab VARCHAR;

BEGIN

FOR con IN SELECT * FROM opentheso_get_concept_son(idtheso, idbt, private)
                             LOOP
                         -- prefLab
    preflab = '';
SELECT * INTO preflab_rec FROM opentheso_get_conceptlabel(idtheso, con.idconcept2, idlang);
preflab = preflab_rec.libelle;


            -- altLab hiden
            havechildren = false;
SELECT opentheso_ishave_children(idtheso, con.idconcept2) INTO havechildren;


SELECT con.idconcept2, con.notation, con.status, prefLab, havechildren INTO rec;

RETURN NEXT rec;
END LOOP;
END;
$$;

alter function opentheso_get_list_narrower_fortree(varchar, varchar, varchar, boolean) owner to opentheso;
