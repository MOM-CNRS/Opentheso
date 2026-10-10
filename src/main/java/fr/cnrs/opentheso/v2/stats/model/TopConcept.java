package fr.cnrs.opentheso.v2.stats.model;

import java.util.List;

public record TopConcept(
        String conceptId,
        String label,
        String thesaurusId,
        String thesaurusLabel,
        long views,
        List<ConceptLangShare> languages
) {
}
