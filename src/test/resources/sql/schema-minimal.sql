-- Schéma minimal pour les tests SQL avec Testcontainers
-- Contient uniquement les tables nécessaires aux requêtes testées

CREATE TABLE concept (
    id_concept  character varying NOT NULL,
    id_thesaurus character varying NOT NULL,
    status      character varying,
    notation    character varying DEFAULT '',
    top_concept boolean          DEFAULT false,
    concept_type text            DEFAULT 'concept',
    id_ark      character varying DEFAULT '',
    created     timestamp with time zone,
    modified    timestamp with time zone,
    creator     integer          DEFAULT -1,
    contributor integer          DEFAULT -1,
    PRIMARY KEY (id_concept, id_thesaurus)
);

CREATE TABLE term (
    id_term      character varying NOT NULL,
    lexical_value character varying NOT NULL,
    lang         character varying NOT NULL,
    id_thesaurus text              NOT NULL,
    created      timestamp with time zone DEFAULT now(),
    modified     timestamp with time zone DEFAULT now(),
    status       character varying DEFAULT 'D',
    PRIMARY KEY (id_term, id_thesaurus, lang)
);

CREATE TABLE preferred_term (
    id_concept   character varying NOT NULL,
    id_term      character varying NOT NULL,
    id_thesaurus character varying NOT NULL,
    PRIMARY KEY (id_concept, id_term, id_thesaurus)
);

CREATE TABLE non_preferred_term (
    id_term      character varying NOT NULL,
    lexical_value character varying NOT NULL,
    lang         character varying NOT NULL,
    id_thesaurus character varying NOT NULL,
    hiden        boolean           DEFAULT false,
    PRIMARY KEY (id_term, id_thesaurus, lang)
);

CREATE TABLE hierarchical_relationship (
    id_concept1  character varying NOT NULL,
    id_thesaurus character varying NOT NULL,
    role         character varying NOT NULL,
    id_concept2  character varying NOT NULL,
    PRIMARY KEY (id_concept1, id_thesaurus, role, id_concept2)
);
