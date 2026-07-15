package fr.cnrs.opentheso.v2.concept.write.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConceptWriteProperties.class)
public class ConceptWriteConfiguration {
}
