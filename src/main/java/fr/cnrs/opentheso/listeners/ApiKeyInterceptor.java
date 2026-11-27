package fr.cnrs.opentheso.listeners;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyMissingException;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;
    private Set<String> allowedMethods;

    public ApiKeyInterceptor(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
        this.allowedMethods = Set.of("POST", "PUT", "DELETE"); // immutable set
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Écritures lancées depuis l'interface : session JSF, pas de clé API.
        if (uri.contains("/reconcile") || isSessionWrite(uri)) {
            return true;
        }

        if (allowedMethods.contains(request.getMethod().toUpperCase())) {

            String apiKey = request.getHeader("X-API-KEY"); // V2

            if (apiKey == null || apiKey.isBlank()) {
                apiKey = request.getHeader("API-KEY"); // fallback V1
            }

            request.setAttribute("apiKey", apiKey);

            if (apiKey == null || apiKey.isBlank()) {
                throw new ApiKeyMissingException();
            }

            var match = apiKeyService.lookup(apiKey)
                    .orElseThrow(() -> new ApiKeyInvalidException(ApiKeyState.INVALID));
            if (!match.valid()) {
                throw new ApiKeyInvalidException(match.state()); // clé expirée : 401
            }
            User user = new User();
            user.setId(match.userId());
            user.setUsername(match.username());
            request.setAttribute("authenticatedUser", user);
        }

        return true;
    }

    static boolean isSessionWrite(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.contains("/api/selection-export")
                || uri.endsWith("/api/account/tree-status")
                || uri.endsWith("/api/account/table-cols")
                || uri.endsWith("/api/account/concept-blocks")
                // Purge du journal : admin du thésaurus vérifié par le contrôleur sur la session
                || uri.endsWith("/api/concepts/history/thesaurus/purge");
    }
}