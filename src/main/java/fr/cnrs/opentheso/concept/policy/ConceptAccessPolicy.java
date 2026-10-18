package fr.cnrs.opentheso.concept.policy;

import org.apache.commons.lang3.StringUtils;

public final class ConceptAccessPolicy {

    private ConceptAccessPolicy() {
    }

    public static boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }
}
