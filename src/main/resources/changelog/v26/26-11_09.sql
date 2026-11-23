-- Un thésaurus exporté porte la fusion dans les deux sens (replaces et isReplacedBy).
-- La seconde écriture retrouve la même clé (id_concept1, id_concept2, id_thesaurus).

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
        SELECT DISTINCT id_con, btrim(idConceptReplaceBy), id_thesaurus, id_user
        FROM unnest(string_to_array(idsConceptsReplaceBy, seperateur)) AS idConceptReplaceBy
        WHERE btrim(idConceptReplaceBy) <> ''
        ON CONFLICT ON CONSTRAINT concept_fusion_pkey DO NOTHING;
    END IF;

    IF (gps IS NOT NULL AND gps != '' AND gps != 'null') THEN
        CALL opentheso_add_gps(id_con, id_thesaurus, gps);
    END IF;
END;
$BODY$;
