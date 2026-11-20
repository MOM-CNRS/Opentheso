-- Marque de langue des blocs Traductions et Notes : drapeau (défaut) ou code à deux lettres.

ALTER TABLE preferences
    ADD COLUMN IF NOT EXISTS language_codes boolean NOT NULL DEFAULT false;
