CREATE TABLE IF NOT EXISTS user_tree_status_pref (
    id_user INTEGER NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    status_id VARCHAR(16) NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_user, status_id)
);
