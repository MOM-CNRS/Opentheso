-- Les notes d'import (concept et facette) étaient écrites dans id_concept / id_term
-- sans identifier. La fiche ne lit que identifier, donc elles restaient invisibles.

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
    SELECT parts[2], id_thesaurus, id_concept, parts[3], parts[1], id_user, id_concept
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('customnote', 'scopeNote', 'note');

    INSERT INTO note (notetypecode, id_thesaurus, id_term, lang, lexicalvalue, id_user, identifier)
    SELECT parts[2], id_thesaurus, parts[4], parts[3], parts[1], id_user, id_concept
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('definition', 'historyNote', 'editorialNote', 'changeNote', 'example');
END;
$BODY$;

UPDATE note
SET identifier = COALESCE(NULLIF(btrim(identifier), ''), id_concept, id_term)
WHERE identifier IS NULL OR btrim(identifier) = '';

UPDATE external_images
SET image_name = ''
WHERE image_name IS NOT NULL AND lower(btrim(image_name)) = 'null';

UPDATE external_images
SET image_copyright = ''
WHERE image_copyright IS NOT NULL
  AND (
      lower(btrim(image_copyright)) = 'null'
      OR btrim(image_copyright) ~* '_(sparql|rest)$'
  );

UPDATE external_images
SET image_creator = ''
WHERE image_creator IS NOT NULL
  AND (
      lower(btrim(image_creator)) = 'null'
      OR btrim(image_creator) ~* '_(sparql|rest)$'
  );
