CREATE TABLE password_reset_token (
                                      id BIGSERIAL PRIMARY KEY,
                                      token VARCHAR(64) NOT NULL UNIQUE,
                                      id_user INTEGER NOT NULL REFERENCES users(id_user),
                                      expires_at TIMESTAMP NOT NULL,
                                      used BOOLEAN NOT NULL DEFAULT FALSE
);