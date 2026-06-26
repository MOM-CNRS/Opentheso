CREATE TABLE sso_tokens (
                            token UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            user_id INTEGER NOT NULL REFERENCES users(id_user),
                            expires_at TIMESTAMP NOT NULL,
                            used BOOLEAN DEFAULT FALSE
);