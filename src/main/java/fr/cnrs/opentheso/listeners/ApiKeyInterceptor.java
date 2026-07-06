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

import java.util.Optional;
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

        // 🔥 EXCLUSION OPENREFINE RECONCILIATION
        if (uri.contains("/reconcile")) {
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

            Optional<User> userOpt = apiKeyService.findUserByApiKey(apiKey);
            if (userOpt.isEmpty()) {
                throw new ApiKeyInvalidException(ApiKeyState.INVALID);
            }

            request.setAttribute("authenticatedUser", userOpt.get());
        }

        return true;
    }
}