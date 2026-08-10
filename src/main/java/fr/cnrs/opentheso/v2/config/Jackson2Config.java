package fr.cnrs.opentheso.v2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Boot 4 configure Jackson 3 ({@code tools.jackson}) par défaut.
 * Plusieurs beans injectent encore {@link ObjectMapper} Jackson 2.
 */
@Configuration
public class Jackson2Config {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
