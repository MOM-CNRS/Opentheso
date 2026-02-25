-- mise à jour de la fonction pour récupérer les traductions dans l'ordre des langues
DROP FUNCTION IF EXISTS public.opentheso_get_preflabel_traductions(character varying, character varying, character varying);

CREATE OR REPLACE FUNCTION public.opentheso_get_preflabel_traductions(
	id_theso character varying,
	id_con character varying,
	id_lang character varying)
    RETURNS TABLE(term_id character varying, term_lexical_value character varying, term_lang character varying, unique_id integer, flag_code character varying)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$
begin
return query
SELECT term.id_term, term.lexical_value, term.lang, term.id, languages_iso639.code_pays
FROM term, preferred_term, languages_iso639
WHERE term.id_term = preferred_term.id_term
  AND term.id_thesaurus = preferred_term.id_thesaurus
  AND term.lang = languages_iso639.iso639_1

  AND term.id_thesaurus = id_theso
  AND preferred_term.id_concept = id_con
  AND term.lang != id_lang
ORDER BY term.lang;

end;
$BODY$;

--
DROP FUNCTION IF EXISTS public.opentheso_get_altlabel_traductions(character varying, character varying, character varying, boolean);
CREATE OR REPLACE FUNCTION public.opentheso_get_altlabel_traductions(
	idtheso character varying,
	idconcept character varying,
	idlang character varying,
	ishiden boolean)
    RETURNS TABLE(altlabel_id character varying, altlabel character varying, idlang_alt character varying, unique_id integer)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$
begin
return query
SELECT non_preferred_term.id_term, non_preferred_term.lexical_value, non_preferred_term.lang, non_preferred_term.id
FROM non_preferred_term, preferred_term
WHERE preferred_term.id_term = non_preferred_term.id_term
  AND preferred_term.id_thesaurus = non_preferred_term.id_thesaurus
  AND preferred_term.id_concept = idconcept
  AND non_preferred_term.id_thesaurus = idtheso
  AND non_preferred_term.lang != idlang
		AND non_preferred_term.hiden = isHiden
ORDER BY non_preferred_term.lang;
end;
$BODY$;

--- Notes
DROP FUNCTION IF EXISTS public.opentheso_get_notes(character varying, character varying);
CREATE OR REPLACE FUNCTION public.opentheso_get_notes(
	id_theso character varying,
	id_con character varying)
    RETURNS TABLE(note_id integer, note_notetypecode text, note_lexicalvalue character varying, note_created timestamp without time zone, note_modified timestamp without time zone, note_lang character varying, note_source character varying)
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
    ROWS 1000

AS $BODY$
begin
return query
SELECT note.id, note.notetypecode, note.lexicalvalue, note.created, note.modified, note.lang, note.notesource
FROM note
WHERE
    note.id_thesaurus = id_theso
  AND note.identifier = id_con
ORDER BY
    note.notetypecode,
    note.lang;
end;
$BODY$;