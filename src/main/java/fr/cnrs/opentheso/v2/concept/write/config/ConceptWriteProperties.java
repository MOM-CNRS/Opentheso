package fr.cnrs.opentheso.v2.concept.write.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opentheso.concept-write")
public record ConceptWriteProperties(
        boolean nativePersistenceEnabled
) {

    public ConceptWriteProperties() {
        this(false);
    }
}
