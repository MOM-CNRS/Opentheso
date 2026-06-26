-- nettoyage des blancs (données historiques invalides)

DELETE FROM public.note
WHERE lang IS NULL
   OR btrim(lang) = ''
   OR id_thesaurus IS NULL
   OR btrim(id_thesaurus) = '';

UPDATE public.note
SET identifier = NULL
WHERE identifier IS NOT NULL
  AND btrim(identifier) = '';

-- protection des identifiants contre les blancs

DO $$
    BEGIN

        -- lang
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'note_lang_not_blank'
        ) THEN
            ALTER TABLE public.note
                ADD CONSTRAINT note_lang_not_blank
                    CHECK (trim(lang) <> '');
        END IF;

        -- id_thesaurus
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'note_id_thesaurus_not_blank'
        ) THEN
            ALTER TABLE public.note
                ADD CONSTRAINT note_id_thesaurus_not_blank
                    CHECK (trim(id_thesaurus) <> '');
        END IF;

        -- identifier (optionnel)
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'note_identifier_not_blank'
        ) THEN
            ALTER TABLE public.note
                ADD CONSTRAINT note_identifier_not_blank
                    CHECK (identifier IS NULL OR trim(identifier) <> '');
        END IF;

    END $$;
