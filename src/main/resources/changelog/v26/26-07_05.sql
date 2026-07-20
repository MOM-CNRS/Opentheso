-- Faster concept import helpers:
-- 1) Skip note/non-pref historiques (noise on bulk import; halves writes for notes/alts)
-- 2) Drop redundant SELECT after INSERT in opentheso_add_new_concept
-- 3) Set-based INSERT … SELECT FROM unnest for hot child procedures

CREATE OR REPLACE PROCEDURE opentheso_add_notes(
    id_concept character varying,
    id_thesaurus character varying,
    id_user int,
    notes text)
    LANGUAGE plpgsql
AS $BODY$
BEGIN
    IF notes IS NULL OR notes = '' OR notes = 'null' THEN
        RETURN;
    END IF;

    -- concept-level notes
    INSERT INTO note (notetypecode, id_thesaurus, id_concept, lang, lexicalvalue, id_user)
    SELECT parts[2], id_thesaurus, id_concept, parts[3], parts[1], id_user
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('customnote', 'scopeNote', 'note');

    -- term-level notes
    INSERT INTO note (notetypecode, id_thesaurus, id_term, lang, lexicalvalue, id_user)
    SELECT parts[2], id_thesaurus, parts[4], parts[3], parts[1], id_user
    FROM (
        SELECT string_to_array(note_value, '@@') AS parts
        FROM unnest(string_to_array(notes, '##')) AS note_value
    ) parsed
    WHERE parts[2] IN ('definition', 'historyNote', 'editorialNote', 'changeNote', 'example');

    -- Historique volontairement omis : ces procédures servent l'import bulk
END;
$BODY$;


CREATE OR REPLACE PROCEDURE opentheso_add_non_preferred_term(
    id_thesaurus character varying,
    id_user int,
    non_pref_terms text)
    LANGUAGE plpgsql
AS $BODY$
BEGIN
    IF non_pref_terms IS NULL OR non_pref_terms = '' OR non_pref_terms = 'null' THEN
        RETURN;
    END IF;

    INSERT INTO non_preferred_term (id_term, lexical_value, lang, id_thesaurus, source, status, hiden)
    SELECT parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], CAST(parts[7] AS BOOLEAN)
    FROM (
        SELECT string_to_array(non_pref_value, '@@') AS parts
        FROM unnest(string_to_array(non_pref_terms, '##')) AS non_pref_value
    ) parsed
    ON CONFLICT DO NOTHING;

    -- Historique volontairement omis pour l'import bulk
END;
$BODY$;


CREATE OR REPLACE PROCEDURE opentheso_add_hierarchical_relations(
    id_thesaurus character varying,
    relations text)
    LANGUAGE plpgsql
AS $BODY$
BEGIN
    IF relations IS NULL OR relations = '' OR relations = 'null' THEN
        RETURN;
    END IF;

    INSERT INTO hierarchical_relationship (id_concept1, id_thesaurus, role, id_concept2)
    SELECT parts[1], id_thesaurus, parts[2], parts[3]
    FROM (
        SELECT string_to_array(relation_value, '@@') AS parts
        FROM unnest(string_to_array(relations, '##')) AS relation_value
    ) parsed
    ON CONFLICT DO NOTHING;
END;
$BODY$;


CREATE OR REPLACE PROCEDURE opentheso_add_terms(
    id_term character varying,
    id_thesaurus character varying,
    id_concept character varying,
    id_user int,
    terms text)
    LANGUAGE plpgsql
AS $BODY$
BEGIN
    IF terms IS NULL OR terms = '' OR terms = 'null' THEN
        RETURN;
    END IF;

    INSERT INTO term (id_term, lexical_value, lang, id_thesaurus, created, modified, source, status, contributor)
    SELECT id_term, parts[1], parts[2], id_thesaurus, CURRENT_DATE, CURRENT_DATE, '', '', id_user
    FROM (
        SELECT string_to_array(term_value, '@@') AS parts
        FROM unnest(string_to_array(terms, '##')) AS term_value
    ) parsed;

    INSERT INTO preferred_term (id_concept, id_term, id_thesaurus)
    VALUES (id_concept, id_term, id_thesaurus);
END;
$BODY$;


CREATE OR REPLACE PROCEDURE opentheso_add_new_concept(
    id_thesaurus character varying,
    id_con character varying,
    id_user int,
    conceptStatus character varying,
    conceptType text,
    notationConcept character varying,
    id_ark character varying,
    isTopConcept Boolean,
    id_handle character varying,
    id_doi character varying,
    prefterms text,
    relation_hiarchique text,
    custom_relation text,
    notes text,
    non_pref_terms text,
    alignements text,
    images text,
    idsConceptsReplaceBy text,
    isGpsPresent Boolean,
    gps text,
    created Date,
    modified Date,
    concept_dcterms text)
    LANGUAGE plpgsql
AS $BODY$
DECLARE
    seperateur constant varchar := '##';
BEGIN
    INSERT INTO concept (
        id_concept, id_thesaurus, id_ark, created, modified, status, concept_type,
        notation, top_concept, id_handle, id_doi, creator, contributor, gps
    )
    VALUES (
        id_con, id_thesaurus, id_ark, created, modified, conceptStatus, conceptType,
        notationConcept, isTopConcept, id_handle, id_doi, id_user, id_user, isGpsPresent
    );

    IF (prefterms IS NOT NULL AND prefterms != 'null') THEN
        CALL opentheso_add_terms(id_con, id_thesaurus, id_con, id_user, prefterms);
    END IF;

    IF (relation_hiarchique IS NOT NULL AND relation_hiarchique != 'null') THEN
        CALL opentheso_add_hierarchical_relations(id_thesaurus, relation_hiarchique);
    END IF;

    IF (custom_relation IS NOT NULL AND custom_relation != 'null') THEN
        CALL opentheso_add_custom_relations(id_thesaurus, custom_relation);
    END IF;

    IF (concept_dcterms IS NOT NULL AND concept_dcterms != 'null') THEN
        CALL opentheso_add_concept_dcterms(id_con, id_thesaurus, concept_dcterms);
    END IF;

    IF (notes IS NOT NULL AND notes != 'null') THEN
        CALL opentheso_add_notes(id_con, id_thesaurus, id_user, notes);
    END IF;

    IF (non_pref_terms IS NOT NULL AND non_pref_terms != 'null') THEN
        CALL opentheso_add_non_preferred_term(id_thesaurus, id_user, non_pref_terms);
    END IF;

    IF (images IS NOT NULL AND images != 'null') THEN
        CALL opentheso_add_external_images(id_thesaurus, id_con, id_user, images);
    END IF;

    IF (alignements IS NOT NULL AND alignements != 'null') THEN
        CALL opentheso_add_alignements(alignements);
    END IF;

    IF (idsConceptsReplaceBy IS NOT NULL AND idsConceptsReplaceBy != 'null') THEN
        INSERT INTO concept_replacedby (id_concept1, id_concept2, id_thesaurus, id_user)
        SELECT id_con, idConceptReplaceBy, id_thesaurus, id_user
        FROM unnest(string_to_array(idsConceptsReplaceBy, seperateur)) AS idConceptReplaceBy;
    END IF;

    IF (gps IS NOT NULL AND gps != '' AND gps != 'null') THEN
        CALL opentheso_add_gps(id_con, id_thesaurus, gps);
    END IF;
END;
$BODY$;
