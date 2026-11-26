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
 * L'API V1 seule peut aussi être coupée : elle répond alors 410 et renvoie vers la V2.
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
        if (!enabled) {
            refuse(response, HttpServletResponse.SC_NOT_FOUND, disabledBody(opentheso));
            return;
        }
        if (opentheso && !features.apiV1Enabled() && PublicApiGate.isApiV1(path)) {
            // 410 Gone : l'adresse a existé, elle est retirée au profit de la V2
            refuse(response, HttpServletResponse.SC_GONE, V1_DISABLED_BODY);
            return;
        }
        filterChain.doFilter(request, response);
    }

    static final String V1_DISABLED_BODY = "{\"message\":\"L'API V1 est désactivée sur cette instance. "
            + "Utilisez l'API V2 : /openapi/v2/public (documentation : /swagger-ui/index.html).\"}";

    private static void refuse(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        noStore(response);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(body);
    }

    /**
     * Un navigateur peut garder en cache un 404 ou un 410 sans en-tête : l'API resterait
     * fermée pour lui après sa réactivation. Ce filtre passe avant Spring Security, qui ajoute d'habitude l'en-tête.
     */
    static void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
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
