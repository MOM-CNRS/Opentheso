-- Origin of a change suggestion: synchronisation vs manual user request.

ALTER TABLE public.proposition_modification
    ADD COLUMN IF NOT EXISTS from_sync boolean NOT NULL DEFAULT false;

UPDATE public.proposition_modification
SET from_sync = true
WHERE from_sync = false
  AND commentaire ILIKE 'Synchronisation depuis%';
