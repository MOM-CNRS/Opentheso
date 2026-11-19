-- Visibilité des entrées de la barre de gauche : proposition, langue, API REST, API graphe.

CREATE TABLE IF NOT EXISTS instance_feature (
    id smallint PRIMARY KEY,
    suggestion_enabled boolean NOT NULL DEFAULT true,
    language_visible boolean NOT NULL DEFAULT true,
    rest_api_visible boolean NOT NULL DEFAULT true,
    graph_api_visible boolean NOT NULL DEFAULT true,
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT instance_feature_singleton CHECK (id = 1)
);

INSERT INTO instance_feature (id)
VALUES (1)
ON CONFLICT (id) DO NOTHING;
