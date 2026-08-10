package fr.cnrs.opentheso.config;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.repositories.UserRepository;
import fr.cnrs.opentheso.v2.shared.session.SessionAuthenticatedUserSource;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.security.keycloak-enabled}")
    private boolean keycloakEnabled;

    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Configuration commune aux deux modes
        http
                .sessionManagement(session -> session.sessionFixation().none())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/login", "/logout", "/oauth2/**",
                                "/javax.faces.resource/**",
                                "/jakarta.faces.resource/**",
                                "/openapi/v1/**",
                                "/api/v2/auth/token"
                        ).permitAll()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/openapi/v1/**",
                                "/api/v2/**",
                                "/api/v2/auth/token"
                        )
                        .ignoringRequestMatchers(
                                request -> request.getServletPath().endsWith(".xhtml")
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                );

        // Configuration spécifique Keycloak
        if (keycloakEnabled) {
            log.info("✅ Keycloak activé - Application en mode authentification SSO");
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .successHandler(authenticationSuccessHandler())
                    .failureHandler(authenticationFailureHandler())
                    .userInfoEndpoint(userInfo -> userInfo
                            .userAuthoritiesMapper(this.userAuthoritiesMapper())));
        } else {
            log.warn("⚠️ Keycloak désactivé - Application en mode authentification locale");
        }

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            var oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            log.debug("Authentification réussie. Email : {}", email);

            HttpSession session = request.getSession(true);
            if (StringUtils.isNotEmpty(email)) {
                var user = userRepository.findByMail(email);
                if (user.isPresent()) {
                    Integer userId = user.get().getId();
                    log.debug("Utilisateur trouvé dans la base Opentheso, préparation de la session SSO …");
                    session.setAttribute("LOGIN_INFO_MESSAGE", "Connexion réussie via KeyCloak");
                    // Consommé ensuite en contexte JSF (CurrentUser.onPageLoad / SsoSessionBridge)
                    session.setAttribute("ssoUserId", userId);
                    session.setAttribute("ssoProcessed", false);
                    // V2 : même clé que SessionAuthenticatedUserSource (pas de FacesContext ici)
                    session.setAttribute(SessionAuthenticatedUserSource.SESSION_USER_ID_KEY, userId);
                } else {
                    session.setAttribute("LOGIN_ERROR_MESSAGE",
                            "Connexion réussie, mais vous n'avez pas de compte dans Opentheso !"
                                    + " demandez à un Admin de vous donner des droits");
                    log.error("Utilisateur avec email : {} non trouvé", email);
                }
            }
            response.sendRedirect(request.getContextPath() + "/");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            response.sendRedirect("/authFailure");
        };
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            for (GrantedAuthority authority : authorities) {
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    Map<String, Object> attributes = oidcUserAuthority.getAttributes();
                    Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
                    if (realmAccess != null) {
                        List<String> roles = (List<String>) realmAccess.get("roles");
                        for (String role : roles) {
                            mappedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                        }
                    }
                }
            }
            return mappedAuthorities;
        };
    }
}