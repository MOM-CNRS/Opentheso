-- Instance dashboard: raw usage events + daily concept-view aggregates.

CREATE TABLE IF NOT EXISTS public.stat_log_event
(
    id                bigserial PRIMARY KEY,
    event_type        varchar(30)  NOT NULL,
    event_time        timestamp    NOT NULL,
    thesaurus_label   varchar(500),
    thesaurus_id      varchar(50),
    concept_id        varchar(50),
    concept_label     varchar(500),
    lang              varchar(10),
    collection_id     varchar(50),
    collection_label  varchar(500),
    url               varchar(500),
    http_method       varchar(10),
    searched_term     varchar(500),
    selected_term     varchar(500),
    nb_results        integer
);

CREATE INDEX IF NOT EXISTS idx_stat_log_event_type_time
    ON public.stat_log_event (event_type, event_time);

CREATE INDEX IF NOT EXISTS idx_stat_log_event_thesaurus_time
    ON public.stat_log_event (thesaurus_id, event_time);

CREATE INDEX IF NOT EXISTS idx_stat_log_event_concept_time
    ON public.stat_log_event (concept_id, event_time)
    WHERE event_type = 'CONCEPT_VIEW';

CREATE INDEX IF NOT EXISTS idx_stat_log_event_search_time
    ON public.stat_log_event (event_time)
    WHERE event_type IN ('SEARCH_NO_RESULT', 'SEARCH_RESULT_SELECTED', 'SEARCH_APPLIED');

CREATE INDEX IF NOT EXISTS idx_stat_log_event_api_time
    ON public.stat_log_event (event_time, url, http_method)
    WHERE event_type = 'API_CALL';

CREATE TABLE IF NOT EXISTS public.stat_concept_view_daily
(
    stat_date         date         NOT NULL,
    concept_id        varchar(50)  NOT NULL,
    thesaurus_id      varchar(50)  NOT NULL,
    concept_label     varchar(500),
    thesaurus_label   varchar(500),
    nb_vues           integer      NOT NULL,
    PRIMARY KEY (stat_date, concept_id, thesaurus_id)
);

CREATE INDEX IF NOT EXISTS idx_stat_concept_view_daily_thesaurus_date
    ON public.stat_concept_view_daily (thesaurus_id, stat_date);
