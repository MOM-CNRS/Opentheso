package fr.cnrs.opentheso.concept.write.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConceptWriteProperties.class)
public class ConceptWriteConfiguration {
}
