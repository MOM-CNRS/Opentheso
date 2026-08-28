CREATE TABLE IF NOT EXISTS user_concept_block_pref (
    id_user INTEGER NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    block_id VARCHAR(32) NOT NULL,
    position INTEGER NOT NULL,
    collapsed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id_user, block_id)
);
