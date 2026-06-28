package fr.cnrs.opentheso.v2.concept.model;

import java.util.List;

public record ConceptDetail(
        String id,
        String thesaurusId,
        String prefLabel,
        String lang,
        String status,
        String arkId,
        String conceptType,
        String created,
        String modified,
        List<BreadcrumbStep> breadcrumb,
        List<ConceptLabel> labels,
        List<ConceptRelationLink> relations,
        List<ConceptNote> notes,
        List<ConceptAlignment> alignments
) {}
