-- Pied de page de la liste des thésaurus : logo et liens publics (0 à N).

CREATE TABLE IF NOT EXISTS picker_footer (
    id smallint PRIMARY KEY,
    brand_label varchar(120) NOT NULL,
    brand_url varchar(500) NOT NULL,
    image bytea,
    image_type varchar(40),
    image_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT picker_footer_singleton CHECK (id = 1)
);

CREATE TABLE IF NOT EXISTS picker_footer_link (
    position integer PRIMARY KEY,
    label varchar(120) NOT NULL,
    url varchar(500) NOT NULL
);

INSERT INTO picker_footer (id, brand_label, brand_url)
VALUES (1, 'Ministère de la Culture', 'https://www.culture.gouv.fr')
ON CONFLICT (id) DO NOTHING;

INSERT INTO picker_footer_link (position, label, url) VALUES
    (0, 'legifrance.gouv.fr', 'https://www.legifrance.gouv.fr'),
    (1, 'info.gouv.fr', 'https://www.info.gouv.fr'),
    (2, 'service-public.fr', 'https://www.service-public.fr'),
    (3, 'data.gouv.fr', 'https://www.data.gouv.fr')
ON CONFLICT (position) DO NOTHING;
