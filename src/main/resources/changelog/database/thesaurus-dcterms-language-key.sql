-- thesaurus_dcterms_uniquekey is UNIQUE (id_thesaurus, name, value). language is missing
-- from the key, so a thesaurus carrying the same dcterms value in two languages cannot be
-- imported at all -- this, from the PACTOLS export, aborts the whole import with 23505:
--
--     <dcterms:title>Pactols_Lieux</dcterms:title>
--     <dcterms:title xml:lang="fr">Pactols_Lieux</dcterms:title>
--
-- A title that reads the same in several languages is the normal case for a proper noun,
-- not an edge case.
--
-- The key did originally include language: it was the primary key, until changeset
-- dropPrimaryKeyThesaurusDcterms (v23.07.3) replaced it with an id column, made language
-- nullable and dropped it from the key in the same step. This restores it.
--
-- Why an expression index and not UNIQUE (id_thesaurus, name, value, language): language
-- is nullable and PostgreSQL treats NULLs as distinct, so untagged rows would escape the
-- constraint entirely. coalesce() folds "no language" into a single slot. (UNIQUE NULLS
-- NOT DISTINCT would also do it, but only on PostgreSQL 15+.)
--
-- No data migration: adding a column to a key only ever accepts more rows than before, so
-- every existing row still satisfies it.

ALTER TABLE thesaurus_dcterms DROP CONSTRAINT IF EXISTS thesaurus_dcterms_uniquekey;
DROP INDEX IF EXISTS thesaurus_dcterms_uniquekey;

CREATE UNIQUE INDEX thesaurus_dcterms_uniquekey
    ON thesaurus_dcterms (id_thesaurus, name, value, coalesce(language, ''));
