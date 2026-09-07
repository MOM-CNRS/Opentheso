package fr.cnrs.opentheso.v2.concept.policy;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public final class ConceptStatusPolicy {

    private ConceptStatusPolicy() {
    }

    public static boolean isDeprecated(String status) {
        return Strings.CI.equals(StringUtils.trimToEmpty(status), "dep");
    }
}
