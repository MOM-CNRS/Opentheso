CREATE TABLE IF NOT EXISTS user_table_col_pref (
    id_user INTEGER NOT NULL REFERENCES users(id_user) ON DELETE CASCADE,
    col_id VARCHAR(16) NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id_user, col_id)
);
