package fr.cnrs.opentheso.v2.concept.policy;

import org.apache.commons.lang3.StringUtils;

public final class ConceptStatusPolicy {

    private ConceptStatusPolicy() {
    }

    public static boolean isDeprecated(String status) {
        return StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(status), "dep");
    }
}
