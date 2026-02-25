package fr.cnrs.opentheso.config;

import fr.cnrs.opentheso.bean.menu.users.CurrentUser;
import fr.cnrs.opentheso.repositories.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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
@ConditionalOnProperty(prefix = "app.security", name = "keycloak-enabled", havingValue = "true")
public class SecurityConfigKeycloak {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(session -> session.sessionFixation().none())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/login", "/logout", "/oauth2/**",
                                "/javax.faces.resource/**",
                                "/openapi/v1/**")
                        .permitAll()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/openapi/v1/**"),
                                request -> request.getServletPath().endsWith(".xhtml")
                        )
                )
                .oauth2Login(oauth2 -> oauth2.loginPage("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                        .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(this.userAuthoritiesMapper())))
                .build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {

            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attr != null) {
                var session = attr.getRequest().getSession(false);
                var oauthUser = (OAuth2User) authentication.getPrincipal();

                String email = oauthUser.getAttribute("email");
                log.debug("Authentification réussie. Email : {}", email);

                if (session != null) {
                    if (StringUtils.isNotEmpty(email)) {
                        // pour Stocker le message dans la session suivant la connexion via SSO
                        HttpSession session2 = request.getSession(true); // IMPORTANT

                        var user = userRepository.findByMail(email);
                        if (user.isPresent()) {
                            log.debug("Utilisateur trouvé dans la base Opentheso, chargement de la session ...");
                            session2.setAttribute("LOGIN_INFO_MESSAGE", "Connexion réussie via KeyCloak");
                            currentUser.setUser(user.get());
                        } else {
                            session2.setAttribute("LOGIN_ERROR_MESSAGE", "Connexion réussie, mais vous n'avez pas de compte dans Opentheso ! demandez à un Admin de vous donner des droits");
                            log.error("Utilisateur avec email : {} non trouvé", email);
                        }
                    }
                }
            }
            response.sendRedirect("/");
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            // Rediriger vers une page d'erreur JSF
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


