DROP PROCEDURE IF EXISTS public.opentheso_add_terms(character varying, character varying, character varying, integer, text);

CREATE OR REPLACE PROCEDURE public.opentheso_add_terms(
    IN id_term character varying,
    IN id_thesaurus character varying,
    IN id_concept character varying,
    IN id_user integer,
    IN terms text)
    LANGUAGE 'plpgsql'
AS $BODY$
DECLARE
    seperateur constant varchar := '##';
    sous_seperateur constant varchar := '@@';
    term_rec record;
    array_string   text[];
BEGIN
    --label.getLabel() + SOUS_SEPERATEUR + label.getLanguage()
    FOR term_rec IN SELECT unnest(string_to_array(terms, seperateur)) AS term_value
        LOOP
            SELECT string_to_array(term_rec.term_value, sous_seperateur) INTO array_string;

            Insert into term (id_term, lexical_value, lang, id_thesaurus, created, modified, source, status, contributor)
            values (id_term, array_string[1], array_string[2], id_thesaurus, CURRENT_DATE, CURRENT_DATE, '', '', id_user)
            ON CONFLICT ON CONSTRAINT term_id_term_key
                DO NOTHING;
        END LOOP;

    -- Insert link term
    Insert into preferred_term (id_concept, id_term, id_thesaurus) values (id_concept, id_term, id_thesaurus)
    ON CONFLICT ON CONSTRAINT preferred_term_pkey
        DO NOTHING;
END;
$BODY$;