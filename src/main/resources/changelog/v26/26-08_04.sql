CREATE INDEX IF NOT EXISTS idx_concept_thesaurus_status
    ON concept (id_thesaurus, status);

CREATE INDEX IF NOT EXISTS idx_hr_thesaurus_concept1_role
    ON hierarchical_relationship (id_thesaurus, id_concept1, role);
