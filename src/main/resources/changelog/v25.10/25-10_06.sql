--index pour optimiser la récupération des concepts fils en fonction de l'option collections privée ou pas
CREATE INDEX idx_cgc_thesaurus_concept ON concept_group_concept (idthesaurus, idconcept);
CREATE INDEX idx_hr_concept2_thesaurus_role ON hierarchical_relationship(id_concept2, id_thesaurus, role);
CREATE INDEX idx_pt_concept_thesaurus_term ON preferred_term(id_concept, id_thesaurus, id_term);
CREATE INDEX idx_term_lang_id_lexval ON term(lang, id_term, lexical_value);
CREATE INDEX idx_thesaurus_array_thesaurus_parent ON thesaurus_array(id_thesaurus, id_concept_parent);
CREATE INDEX idx_concept_facet_facet_thesaurus ON concept_facet(id_facet, id_thesaurus);
CREATE INDEX idx_cgc_idconcept ON concept_group_concept(idconcept);
CREATE INDEX idx_concept_active ON concept(id_concept, id_thesaurus) WHERE status != 'CA';