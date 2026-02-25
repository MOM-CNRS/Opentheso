package fr.cnrs.opentheso.models.skos;

public enum ResourceType {
    FOAF_IMAGE("foaf:Image"),
    CONCEPT("skos:Concept"),
    COLLECTION("skos:Collection"),
    CANDIDATE("skos:candidate"),
    DEPRECATED("owl:deprecated"),
    IS_REPLACED_BY("dcterms:isReplacedBy"),
    REPLACES("dcterms:replace");

    private final String rdfName;

    ResourceType(String rdfName) {
        this.rdfName = rdfName;
    }

    public String getRdfName() {
        return rdfName;
    }

    public static ResourceType fromRdfName(String name) {
        if (name == null) return null;
        for (ResourceType type : values()) {
            if (type.rdfName.equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }
}

