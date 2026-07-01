
DELETE FROM public.note
WHERE btrim(lexicalvalue) = '';

ALTER TABLE public.note
    ADD CONSTRAINT note_lexicalvalue_not_blank
        CHECK (btrim(lexicalvalue) <> '');