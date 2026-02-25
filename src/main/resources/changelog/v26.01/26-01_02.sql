ALTER TABLE preferences
    ADD COLUMN use_openark BOOLEAN DEFAULT FALSE,
    ADD COLUMN server_openark VARCHAR(255) DEFAULT 'http://ark.mondomaine.fr/ark:/',
    ADD COLUMN naan_openark VARCHAR(20) NOT NULL DEFAULT '66666',
    ADD COLUMN prefix_openark VARCHAR(50) NOT NULL DEFAULT 'crt',
    ADD COLUMN api_key_openark VARCHAR(255);