-- ============================================================
-- Table principale : événements de log
-- ============================================================
CREATE TABLE IF NOT EXISTS stat_log_event (
                                              id            BIGSERIAL PRIMARY KEY,
                                              event_type    VARCHAR(30)  NOT NULL,
                                              event_time    TIMESTAMP    NOT NULL,
                                              thesaurus_label VARCHAR(500),
                                              thesaurus_id    VARCHAR(50),
                                              concept_id    VARCHAR(50),
                                              concept_label VARCHAR(500),
                                              lang          VARCHAR(10),
                                              collection_id VARCHAR(50),
                                              collection_label   VARCHAR(500),
                                              url           VARCHAR(500),
                                              http_method   VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_stat_log_event_time
    ON stat_log_event (event_time);
CREATE INDEX IF NOT EXISTS idx_stat_log_event_type_time
    ON stat_log_event (event_type, event_time);
CREATE INDEX IF NOT EXISTS idx_stat_log_event_thesaurus_time
    ON stat_log_event (thesaurus_label, event_time);
CREATE INDEX IF NOT EXISTS idx_stat_log_event_concept
    ON stat_log_event (concept_id, event_time);
CREATE INDEX IF NOT EXISTS idx_stat_log_event_url_time
    ON stat_log_event (url, event_time)
    WHERE event_type = 'API_CALL';

-- ============================================================
-- Table d'agrégats journaliers
-- ============================================================
CREATE TABLE IF NOT EXISTS stat_concept_view_daily (
                                                       stat_date     DATE         NOT NULL,
                                                       concept_id    VARCHAR(50)  NOT NULL,
                                                       thesaurus_id  VARCHAR(50)  NOT NULL,
                                                       nb_vues       INTEGER      NOT NULL,
                                                       PRIMARY KEY (stat_date, concept_id, thesaurus_id)
);

CREATE INDEX IF NOT EXISTS idx_stat_concept_view_daily_thesaurus_date
    ON stat_concept_view_daily (thesaurus_id, stat_date);
CREATE INDEX IF NOT EXISTS idx_stat_concept_view_daily_concept_date
    ON stat_concept_view_daily (concept_id, stat_date);