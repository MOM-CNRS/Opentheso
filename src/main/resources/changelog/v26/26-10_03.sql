-- The unique key included the full Dublin Core value. A long description
-- exceeds the btree row limit and aborts the thesaurus import.

ALTER TABLE public.thesaurus_dcterms DROP CONSTRAINT IF EXISTS thesaurus_dcterms_uniquekey;
DROP INDEX IF EXISTS public.thesaurus_dcterms_uniquekey;

CREATE UNIQUE INDEX thesaurus_dcterms_uniquekey
    ON public.thesaurus_dcterms (id_thesaurus, name, md5(value));
