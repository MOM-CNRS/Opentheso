package fr.cnrs.opentheso.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Opentheso API", version = "2.0"),
        tags = {
                @Tag(name = "V1", description = "Endpoints legacy (openapi v1, api v2 historique, api REST)"),
                @Tag(name = "V2", description = "Nouvelle API de réécriture (modules fr.cnrs.opentheso.v2)")
        },
        security = @SecurityRequirement(name = "apiKey")
)
@SecurityScheme(
        name = "apiKey",
        description = "Clé d'API legacy (en-tête API-KEY)",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "API-KEY"
)
public class OpenApiConfig {

    private static final String V2_SECURITY_SCHEME = "ApiKeyAuth";

    @Bean
    public io.swagger.v3.oas.models.OpenAPI customOpenAPI() {
        return new io.swagger.v3.oas.models.OpenAPI()
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("apiKey",
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("API-KEY")
                                        .description("Clé d'API legacy (V1)")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER))
                        .addSecuritySchemes(V2_SECURITY_SCHEME,
                                new io.swagger.v3.oas.models.security.SecurityScheme()
                                        .name("X-API-KEY")
                                        .description("Clé d'API V2 (API-KEY accepté en fallback)")
                                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER)));
    }

    /**
     * V1 — tous les endpoints legacy existants avant la réécriture v2.
     */
    @Bean
    public GroupedOpenApi apiV1OpenApi() {
        return GroupedOpenApi.builder()
                .group("V1")
                .pathsToMatch(
                        "/openapi/v1/**",
                        "/api/v2/**",
                        "/api/**",
                        "/Auth"
                )
                .pathsToExclude("/openapi/v2/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("Opentheso — API V1 (legacy)")
                            .version("1.0")
                            .description("""
                                    Endpoints historiques d'OpenTheso :
                                    - `/openapi/v1/**` : API OpenAPI documentée (concepts, thésaurus, groupes, users…)
                                    - `/api/v2/**` : API concepts v2 historique (ajout, mise à jour, réconciliation)
                                    - `/api/**` : API REST publique (ARK, recherche, export RDF/JSON…)
                                    - `/Auth` : test d'authentification par clé API

                                    Authentification : en-tête `API-KEY`.
                                    """));
                    openApi.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                            .addList("apiKey"));
                })
                .build();
    }

    /**
     * V2 — nouvelle API construite module par module (fr.cnrs.opentheso.v2).
     * Préfixe URL : /openapi/v2/**
     */
    @Bean
    public GroupedOpenApi apiV2OpenApi() {
        return GroupedOpenApi.builder()
                .group("V2")
                .pathsToMatch("/openapi/v2/**")
                .addOpenApiCustomizer(openApi -> {
                    openApi.info(new io.swagger.v3.oas.models.info.Info()
                            .title("Opentheso — API V2")
                            .version("2.0")
                            .description("""
                                    Nouvelle API de la réécriture OpenTheso (architecture modulaire v2).
                                    Chaque module fonctionnel expose ses endpoints sous `/openapi/v2/`.

                                    Modules disponibles :
                                    - `account` : Mon compte (profil, mot de passe, rôles, clé API)
                                    - `projects` : Mes projets/utilisateurs (projets, membres, rôles, thésaurus)
                                    - `admin` : Administration super-admin (tous les utilisateurs, projets, thésaurus)

                                    Authentification : en-tête `X-API-KEY` (ou `API-KEY` en fallback).
                                    """));
                    openApi.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                            .addList(V2_SECURITY_SCHEME));
                })
                .build();
    }
}
