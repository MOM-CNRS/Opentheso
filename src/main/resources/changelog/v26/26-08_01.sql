CREATE INDEX IF NOT EXISTS idx_role_only_on_user_theso ON user_role_only_on (id_user, id_theso);
CREATE INDEX IF NOT EXISTS idx_group_thesaurus_theso ON user_group_thesaurus (id_thesaurus);
CREATE INDEX IF NOT EXISTS idx_role_group_user_group ON user_role_group (id_user, id_group);