-- FUNCTION: public.opentheso_get_facets_of_concept(character varying, character varying, character varying)

DROP FUNCTION IF EXISTS public.opentheso_get_facets_of_concept(character varying, character varying, character varying);

CREATE OR REPLACE FUNCTION public.opentheso_get_facets_of_concept(
	id_theso character varying,
	id_con character varying,
	id_lang character varying)
    RETURNS SETOF record
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$
DECLARE
rec record;
	facet_rec record;

	libelle character varying;
	have_members boolean;

BEGIN
    FOR facet_rec IN
        SELECT id_facet
        FROM thesaurus_array ta
        WHERE id_thesaurus = id_theso
          AND (ta.id_concept_parent = id_con
            OR ta.id_concept_parent = (SELECT id_ark FROM concept WHERE id_concept = id_con AND id_thesaurus = id_theso))
        LOOP
            libelle = '';
            SELECT opentheso_get_labelfacet(id_theso, facet_rec.id_facet, id_lang) INTO libelle;

            SELECT opentheso_isfacet_hasmembers(id_theso, facet_rec.id_facet) INTO have_members;

            SELECT facet_rec.id_facet, libelle, have_members INTO rec;

            RETURN NEXT rec;
        END LOOP;
    END;
$BODY$;

