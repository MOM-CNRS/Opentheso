-- Pages du pied de page (mentions, données personnelles, accessibilité), par langue.

CREATE TABLE IF NOT EXISTS legal_page (
    code varchar(32) NOT NULL,
    lang varchar(8) NOT NULL,
    title varchar(400) NOT NULL,
    htmlcode text,
    PRIMARY KEY (code, lang)
);
