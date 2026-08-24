-- ============================================================
-- Table principale : événements de log
-- ============================================================
CREATE TABLE IF NOT EXISTS public.stat_log_event
(
    id bigserial NOT NULL,
    event_type character varying(30) COLLATE pg_catalog."default" NOT NULL,
    event_time timestamp without time zone NOT NULL,
    thesaurus_label character varying(500) COLLATE pg_catalog."default",
    thesaurus_id character varying(50) COLLATE pg_catalog."default",
    concept_id character varying(50) COLLATE pg_catalog."default",
    concept_label character varying(500) COLLATE pg_catalog."default",
    lang character varying(10) COLLATE pg_catalog."default",
    collection_id character varying(50) COLLATE pg_catalog."default",
    collection_label character varying(500) COLLATE pg_catalog."default",
    url character varying(500) COLLATE pg_catalog."default",
    http_method character varying(10) COLLATE pg_catalog."default",
    searched_term character varying(500) COLLATE pg_catalog."default",
    selected_term character varying(500) COLLATE pg_catalog."default",
    nb_results integer,
    CONSTRAINT stat_log_event_pkey PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_stat_log_event_api_time
    ON public.stat_log_event USING btree
        (event_time ASC NULLS LAST, url COLLATE pg_catalog."default" ASC NULLS LAST, http_method COLLATE pg_catalog."default" ASC NULLS LAST)
    TABLESPACE pg_default
    WHERE event_type::text = 'API_CALL'::text;
-- Index: idx_stat_log_event_concept_time

-- DROP INDEX IF EXISTS public.idx_stat_log_event_concept_time;

CREATE INDEX IF NOT EXISTS idx_stat_log_event_concept_time
    ON public.stat_log_event USING btree
        (concept_id COLLATE pg_catalog."default" ASC NULLS LAST, event_time ASC NULLS LAST)
    TABLESPACE pg_default;
-- Index: idx_stat_log_event_searched_term_time

-- DROP INDEX IF EXISTS public.idx_stat_log_event_searched_term_time;

CREATE INDEX IF NOT EXISTS idx_stat_log_event_searched_term_time
    ON public.stat_log_event USING btree
        (searched_term COLLATE pg_catalog."default" ASC NULLS LAST, event_time ASC NULLS LAST)
    TABLESPACE pg_default
    WHERE event_type::text = 'SEARCH_QUERY'::text;
-- Index: idx_stat_log_event_thesaurus_time

-- DROP INDEX IF EXISTS public.idx_stat_log_event_selected_term_time;

CREATE INDEX IF NOT EXISTS idx_stat_log_event_selected_term_time
    ON public.stat_log_event USING btree
        (selected_term COLLATE pg_catalog."default" ASC NULLS LAST, event_time ASC NULLS LAST)
    TABLESPACE pg_default
    WHERE event_type::text = 'SEARCH_QUERY'::text;
-- Index: idx_stat_log_event_thesaurus_time

-- DROP INDEX IF EXISTS public.idx_stat_log_event_thesaurus_time;

CREATE INDEX IF NOT EXISTS idx_stat_log_event_thesaurus_time
    ON public.stat_log_event USING btree
        (thesaurus_id COLLATE pg_catalog."default" ASC NULLS LAST, event_time ASC NULLS LAST)
    TABLESPACE pg_default;
-- Index: idx_stat_log_event_type_time

-- DROP INDEX IF EXISTS public.idx_stat_log_event_type_time;

CREATE INDEX IF NOT EXISTS idx_stat_log_event_type_time
    ON public.stat_log_event USING btree
        (event_type COLLATE pg_catalog."default" ASC NULLS LAST, event_time ASC NULLS LAST)
    TABLESPACE pg_default;
-- Index: idx_stat_log_event_zero_results

-- DROP INDEX IF EXISTS public.idx_stat_log_event_zero_results;

CREATE INDEX IF NOT EXISTS idx_stat_log_event_zero_results
    ON public.stat_log_event USING btree
        (event_time ASC NULLS LAST)
    TABLESPACE pg_default
    WHERE event_type::text = 'SEARCH_QUERY'::text AND nb_results = 0;


-- ============================================================
-- Table d'agrégats journaliers
-- ============================================================
CREATE TABLE IF NOT EXISTS stat_concept_view_daily (
                                                       stat_date     DATE         NOT NULL,
                                                       concept_id    VARCHAR(50)  NOT NULL,
                                                       thesaurus_id  VARCHAR(50)  NOT NULL,
                                                       concept_label    VARCHAR(500),
                                                       thesaurus_label  VARCHAR(500),
                                                       nb_vues       INTEGER      NOT NULL,
                                                       PRIMARY KEY (stat_date, concept_id, thesaurus_id)
);

CREATE INDEX IF NOT EXISTS idx_stat_concept_view_daily_thesaurus_date
    ON stat_concept_view_daily (thesaurus_id, stat_date);
CREATE INDEX IF NOT EXISTS idx_stat_concept_view_daily_concept_date
    ON stat_concept_view_daily (concept_id, stat_date);