-- ponytail: the baseline dump's setval() values are stale (e.g. languages_id_seq -> 193 while
-- max(id) is 194), so the first nextval() in a later changeset collides on the primary key.
-- Advance every serial/identity sequence in the schema to at least max(column) + 1.
--
-- Only ever advances. A sequence that is already ahead of max() is left alone: deleted rows
-- leave gaps, and rewinding onto those gaps hands out ids that were already published in
-- ARKs, handles and exported RDF. The changeset is deliberately NOT runAlways for the same
-- reason -- it repairs the baseline dump once, while no instance is serving traffic.
DO $$
DECLARE
    r  record;
    mx bigint;
BEGIN
    FOR r IN
        -- the dump declares sequences without OWNED BY, so pg_get_serial_sequence()
        -- returns NULL for them; fall back to parsing the column default.
        SELECT COALESCE(
                   pg_get_serial_sequence(quote_ident(table_name), column_name),
                   substring(column_default from 'nextval\(''([^'']+)''')
               ) AS seq,
               table_name AS tbl,
               column_name AS col
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND (is_identity = 'YES' OR column_default LIKE 'nextval(%')
    LOOP
        CONTINUE WHEN r.seq IS NULL;
        EXECUTE format('SELECT COALESCE(max(%I), 0) FROM %I', r.col, r.tbl) INTO mx;
        -- nextval() is atomic and never goes backwards, so GREATEST() pins the sequence
        -- forward even if a writer consumed a value since the max() above.
        PERFORM setval(r.seq::regclass,
                       GREATEST(nextval(r.seq::regclass), mx + 1),
                       false);
    END LOOP;
END $$;
