package fr.cnrs.opentheso.config;

import fr.cnrs.opentheso.listeners.ApiKeyInterceptor;
import fr.cnrs.opentheso.stats.intercept.ApiStatInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApiKeyInterceptor apiKeyInterceptor; // ✅ injecter ton interceptor API Key
    @Autowired
    private ApiStatInterceptor apiStatInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(apiStatInterceptor)
                .addPathPatterns("/api/**", "/openapi/**");

        // Clé API pour les écritures /api/** (POST, PUT, DELETE).
        // L'export lancé depuis l'interface utilise la session, pas une clé API.
        registry.addInterceptor(apiKeyInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/selection-export", "/api/selection-export/**");
    }

    // CORS
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false);
            }
        };
    }
}
