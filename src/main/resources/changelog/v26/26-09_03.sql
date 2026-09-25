ALTER TABLE preferences ADD COLUMN IF NOT EXISTS portal_url TEXT;
ALTER TABLE preferences ADD COLUMN IF NOT EXISTS portal_api_key TEXT;
ALTER TABLE preferences ADD COLUMN IF NOT EXISTS portal_acronym TEXT;
ALTER TABLE preferences ADD COLUMN IF NOT EXISTS portal_username TEXT;
ALTER TABLE preferences ADD COLUMN IF NOT EXISTS portal_contact_name TEXT;
ALTER TABLE preferences ADD COLUMN IF NOT EXISTS portal_contact_email TEXT;
ALTER TABLE preferences ADD COLUMN IF NOT EXISTS portal_last_sync_at TIMESTAMP;
