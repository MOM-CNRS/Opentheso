package fr.cnrs.opentheso.v2.toolbox.model;

/**
 * Rôle d'un thésaurus dans une relation maître / esclave.
 */
public enum ThesaurusRole {
    MASTER("master"),
    SLAVE("slave");

    public static final String SKOS_PROPERTY_LOCAL_NAME = "thesaurusRole";
    public static final String SKOS_NAMESPACE = "http://purl.org/umu/uneskos#";

    private final String skosValue;

    ThesaurusRole(String skosValue) {
        this.skosValue = skosValue;
    }

    public String skosValue() {
        return skosValue;
    }

    public boolean isMaster() {
        return this == MASTER;
    }

    public static ThesaurusRole fromMasterFlag(boolean master) {
        return master ? MASTER : SLAVE;
    }

    public static ThesaurusRole fromSkosValue(String value) {
        if (value == null) {
            return SLAVE;
        }
        if ("master".equalsIgnoreCase(value.trim())) {
            return MASTER;
        }
        return SLAVE;
    }
}
