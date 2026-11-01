-- Une langue vide ou le littéral « null » devenait NULL et annulait
-- l'insertion de la note (colonne lang NOT NULL), donc le concept ou la facette
-- était créé sans sa note.

CREATE OR REPLACE PROCEDURE opentheso_add_notes(
    id_concept character varying,
    id_thesaurus character varying,
    id_user int,
    notes text)
    LANGUAGE plpgsql
AS $BODY$
BEGIN
    IF notes IS NULL OR notes = '' OR notes = 'null' THEN
        RETURN;
    END IF;

    INSERT INTO note (notetypecode, id_thesaurus, id_concept, lang, lexicalvalue, id_user, identifier)
    SELECT parts[2], id_thesaurus, id_concept,
           CASE
               WHEN parts[3] IS NULL OR btrim(parts[3]) = '' OR lower(btrim(parts[3])) = 'null' THEN ''
               ELSE btrim(parts[3])
           END,
           parts[1], id_user, id_concept
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('customnote', 'scopeNote', 'note');

    INSERT INTO note (notetypecode, id_thesaurus, id_concept, id_term, lang, lexicalvalue, id_user, identifier)
    SELECT parts[2], id_thesaurus, id_concept, NULLIF(btrim(parts[4]), ''),
           CASE
               WHEN parts[3] IS NULL OR btrim(parts[3]) = '' OR lower(btrim(parts[3])) = 'null' THEN ''
               ELSE btrim(parts[3])
           END,
           parts[1], id_user, id_concept
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('definition', 'historyNote', 'editorialNote', 'changeNote', 'example');
END;
$BODY$;

UPDATE note
SET lang = ''
WHERE lang IS NOT NULL AND lower(btrim(lang)) = 'null';
