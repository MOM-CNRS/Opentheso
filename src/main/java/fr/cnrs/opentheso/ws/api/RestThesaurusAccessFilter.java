package fr.cnrs.opentheso.ws.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Refuse la consultation et la modification d'un thésaurus hors du périmètre de la clé API.
 * Les thésaurus désignés par ARK ou Handle sont résolus avant le contrôle.
 * Le message distingue un thésaurus absent, un thésaurus privé et un thésaurus d'un autre projet.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class RestThesaurusAccessFilter extends OncePerRequestFilter {

    private final RestThesaurusVisibility visibility;
    private final ThesaurusApiTargetResolver targetResolver;

    public RestThesaurusAccessFilter(RestThesaurusVisibility visibility, ThesaurusApiTargetResolver targetResolver) {
        this.visibility = visibility;
        this.targetResolver = targetResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String context = request.getContextPath();
        if (context != null && !context.isEmpty() && path.startsWith(context)) {
            path = path.substring(context.length());
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        // Avec les thésaurus cités par paramètre (?id=), ARK ou Handle, absents des chemins d'origine
        Set<String> thesaurusIds = new LinkedHashSet<>(RestThesaurusTargets.thesaurusIds(path, request::getParameterValues));
        if (!"OPTIONS".equals(request.getMethod())) {
            thesaurusIds.addAll(targetResolver.thesaurusIds(request));
        }
        if (!thesaurusIds.isEmpty()) {
            Integer userId = visibility.currentUserId(request);
            for (String thesaurusId : thesaurusIds) {
                RestThesaurusAccess access = visibility.explain(userId, thesaurusId);
                if (access != RestThesaurusAccess.ALLOWED) {
                    response.setStatus(access.status());
                    PublicApiGateFilter.noStore(response);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write(access.body());
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
