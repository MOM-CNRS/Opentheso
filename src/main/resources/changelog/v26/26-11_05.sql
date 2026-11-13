-- Bandeau de la liste des thésaurus (logo, texte, lien), une seule ligne.

CREATE TABLE IF NOT EXISTS picker_header (
    id smallint PRIMARY KEY,
    visible boolean NOT NULL DEFAULT true,
    link_label varchar(120) NOT NULL,
    link_url varchar(500) NOT NULL,
    image bytea,
    image_type varchar(40),
    image_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT picker_header_singleton CHECK (id = 1)
);

INSERT INTO picker_header (id, visible, link_label, link_url)
VALUES (1, true, 'culture.gouv.fr', 'https://www.culture.gouv.fr')
ON CONFLICT (id) DO NOTHING;
