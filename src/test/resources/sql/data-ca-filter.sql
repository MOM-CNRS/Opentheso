-- Données de test pour la vérification du filtre status != 'CA'

-- Concepts
INSERT INTO concept (id_concept, id_thesaurus, status) VALUES
    ('C_ACTIVE', 'TH1', 'D'),
    ('C_CA',     'TH1', 'CA'),
    ('C_DEP',    'TH1', 'DEP'),
    ('C_CHILD',  'TH1', 'D'),
    ('B_ACTIVE', 'TH1', 'D'),
    ('B_CA',     'TH1', 'CA');

-- Termes
INSERT INTO term (id_term, lexical_value, lang, id_thesaurus) VALUES
    ('T1', 'Terme actif',           'fr', 'TH1'),
    ('T2', 'Concept candidat',      'fr', 'TH1'),
    ('T3', 'Terme déprécié',        'fr', 'TH1'),
    ('T4', 'Terme actif parent',    'fr', 'TH1'),
    ('T5', 'Concept CA parent',     'fr', 'TH1');

-- Termes préférés
INSERT INTO preferred_term (id_concept, id_term, id_thesaurus) VALUES
    ('C_ACTIVE', 'T1', 'TH1'),
    ('C_CA',     'T2', 'TH1'),
    ('C_DEP',    'T3', 'TH1'),
    ('B_ACTIVE', 'T4', 'TH1'),
    ('B_CA',     'T5', 'TH1');

-- Relations hiérarchiques : C_CHILD a deux parents (un actif, un CA)
INSERT INTO hierarchical_relationship (id_concept1, id_thesaurus, role, id_concept2) VALUES
    ('C_CHILD', 'TH1', 'BT', 'B_ACTIVE'),
    ('C_CHILD', 'TH1', 'BT', 'B_CA');
