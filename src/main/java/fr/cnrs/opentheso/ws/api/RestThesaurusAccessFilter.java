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
import java.util.List;

/**
 * Refuse la consultation et la modification d'un thésaurus hors du périmètre de la clé API.
 * Le message distingue un thésaurus absent, un thésaurus privé et un thésaurus d'un autre projet.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class RestThesaurusAccessFilter extends OncePerRequestFilter {

    private final RestThesaurusVisibility visibility;

    public RestThesaurusAccessFilter(RestThesaurusVisibility visibility) {
        this.visibility = visibility;
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
        List<String> thesaurusIds = RestThesaurusTargets.thesaurusIds(path, request::getParameterValues);
        if (!thesaurusIds.isEmpty()) {
            Integer userId = visibility.currentUserId(request);
            for (String thesaurusId : thesaurusIds) {
                RestThesaurusAccess access = visibility.explain(userId, thesaurusId);
                if (access != RestThesaurusAccess.ALLOWED) {
                    response.setStatus(access.status());
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
