CREATE INDEX IF NOT EXISTS concept_replacedby_id_concept2_idx
    ON public.concept_replacedby (id_concept2, id_thesaurus);
