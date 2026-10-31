package fr.cnrs.opentheso.ws.api;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ouvre le thésaurus local depuis {@code /api/ark:/naan/id}.
 * Le navigateur envoie {@code Accept: text/html}, que le mapping JSON ne sélectionne pas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
@RequiredArgsConstructor
public class ArkBrowseRedirectFilter extends OncePerRequestFilter {

    static final Pattern ARK_PATH = Pattern.compile(
            "^/(?:api|openapi/v1/redirect|openapi/v2/public/redirect)/ark:/?([^/]+)/([^/]+)$");

    private final RestRDFHelper restRDFHelper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod()) && !"HEAD".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = applicationPath(request);
        Matcher matcher = ARK_PATH.matcher(path);
        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }
        String target = restRDFHelper.getUrlFromIdArk(matcher.group(1), matcher.group(2));
        if (target == null || target.isBlank()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if ("/".equals(context)) {
            context = "";
        }
        response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        response.setHeader("Location", context + target);
    }

    static String applicationPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if (uri != null && context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        return uri == null || uri.isEmpty() ? "/" : uri;
    }
}
