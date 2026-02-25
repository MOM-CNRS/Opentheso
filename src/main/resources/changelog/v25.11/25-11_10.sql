DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'term'
        AND column_name = 'lexical_value_norm'
    ) THEN
ALTER TABLE term
    ADD COLUMN lexical_value_norm text
        GENERATED ALWAYS AS (f_unaccent(lower(lexical_value))) STORED;
END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE tablename = 'term'
        AND indexname = 'idx_term_lexical_value_norm_trgm'
    ) THEN
        EXECUTE 'CREATE INDEX idx_term_lexical_value_norm_trgm
                 ON term USING gin (lexical_value_norm gin_trgm_ops)';
END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'non_preferred_term'
        AND column_name = 'lexical_value_norm'
    ) THEN
ALTER TABLE non_preferred_term
    ADD COLUMN lexical_value_norm text
        GENERATED ALWAYS AS (f_unaccent(lower(lexical_value))) STORED;
END IF;
END;
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_indexes
        WHERE tablename = 'non_preferred_term'
        AND indexname = 'idx_non_preferred_term_lexical_value_norm_trgm'
    ) THEN
        EXECUTE 'CREATE INDEX idx_non_preferred_term_lexical_value_norm_trgm
                 ON non_preferred_term USING gin (lexical_value_norm gin_trgm_ops)';
END IF;
END;
$$;