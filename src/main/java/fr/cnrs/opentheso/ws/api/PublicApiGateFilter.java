package fr.cnrs.opentheso.ws.api;

import fr.cnrs.opentheso.suggestion.service.InstanceFeatureService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Coupe l'API Opentheso et l'API graphe quand l'interrupteur correspondant
 * est désactivé dans Paramètres de l'instance. L'adresse répond 404 avec la raison.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
@RequiredArgsConstructor
public class PublicApiGateFilter extends OncePerRequestFilter {

    private final InstanceFeatureService instanceFeatureService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = applicationPath(request);
        boolean opentheso = PublicApiGate.isOpenthesoApi(path);
        boolean graph = !opentheso && PublicApiGate.isGraphApi(path);
        if (!opentheso && !graph) {
            filterChain.doFilter(request, response);
            return;
        }
        var features = instanceFeatureService.current();
        boolean enabled = opentheso ? features.restApiVisible() : features.graphApiVisible();
        if (enabled) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(disabledBody(opentheso));
    }

    static String disabledBody(boolean openthesoApi) {
        String message = openthesoApi
                ? "L'API Opentheso est désactivée dans les paramètres de l'instance."
                : "L'API graphe est désactivée dans les paramètres de l'instance.";
        return "{\"message\":\"" + message + "\"}";
    }

    static String applicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if (uri != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        return uri == null || uri.isEmpty() ? "/" : uri;
    }
}
