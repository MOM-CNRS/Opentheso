-- Notes importées avant l'identifiant, et légendes d'image « null » / source d'alignement.
-- Les définitions étaient écrites seulement dans id_term : la fiche ne les retrouvait pas.

UPDATE note
SET identifier = COALESCE(NULLIF(btrim(identifier), ''), id_concept, id_term)
WHERE identifier IS NULL OR btrim(identifier) = '' OR lower(btrim(identifier)) = 'null';

UPDATE note n
SET id_concept = c.id_concept
FROM concept c
WHERE n.id_thesaurus = c.id_thesaurus
  AND (n.id_concept IS NULL OR btrim(n.id_concept) = '')
  AND c.id_concept = COALESCE(NULLIF(btrim(n.identifier), ''), NULLIF(btrim(n.id_term), ''));

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
    SELECT parts[2], id_thesaurus, id_concept, NULLIF(btrim(parts[3]), ''), parts[1], id_user, id_concept
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('customnote', 'scopeNote', 'note');

    INSERT INTO note (notetypecode, id_thesaurus, id_concept, id_term, lang, lexicalvalue, id_user, identifier)
    SELECT parts[2], id_thesaurus, id_concept, NULLIF(btrim(parts[4]), ''), NULLIF(btrim(parts[3]), ''), parts[1], id_user, id_concept
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('definition', 'historyNote', 'editorialNote', 'changeNote', 'example');
END;
$BODY$;

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
