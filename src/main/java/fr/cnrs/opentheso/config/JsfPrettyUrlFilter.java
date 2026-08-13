package fr.cnrs.opentheso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Forwards pretty URLs to JSF views before Spring MVC.
 * Spring view controllers only accept GET; JSF partial AJAX posts to the browser URL (POST).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JsfPrettyUrlFilter extends OncePerRequestFilter {

    private static final Map<String, String> FORWARD_TARGETS = Map.ofEntries(
            Map.entry("/", "/v2/thesaurus/browse.xhtml"),
            Map.entry("/reset-password", "/reset-password.xhtml"),
            Map.entry("/profile", "/profile/myAccount.xhtml"),
            Map.entry("/v2/profile", "/v2/user/my-account.xhtml"),
            Map.entry("/v2/projects-users", "/v2/project/my-projects.xhtml"),
            Map.entry("/v2/admin/users", "/v2/admin/all-users.xhtml"),
            Map.entry("/v2/admin/projects", "/v2/admin/all-projects.xhtml"),
            Map.entry("/v2/admin/thesauri", "/v2/admin/all-thesauri.xhtml"),
            Map.entry("/v2/settings/preference", "/v2/setting/preference.xhtml"),
            Map.entry("/v2/settings/identifier", "/v2/setting/identifier.xhtml"),
            Map.entry("/v2/settings/corpus", "/v2/setting/corpus.xhtml"),
            Map.entry("/v2/toolbox/edition", "/v2/toolbox/edition.xhtml"),
            Map.entry("/v2/toolbox/flags", "/v2/toolbox/flag.xhtml"),
            Map.entry("/v2/toolbox/workshop", "/v2/toolbox/workshop.xhtml"),
            Map.entry("/v2/toolbox/maintenance", "/v2/toolbox/maintenance.xhtml"),
            Map.entry("/v2/toolbox/statistics", "/v2/toolbox/statistics.xhtml"),
            Map.entry("/candidat", "/candidat/candidat.xhtml"),
            Map.entry("/v2/candidat", "/v2/candidat/candidat.xhtml"),
            Map.entry("/v2/graph", "/v2/graph/graph.xhtml"),
            Map.entry("/v2/graph/visualize/thesaurus", "/v2/graph/visualize/thesaurus.xhtml"),
            Map.entry("/v2/graph/visualize/branch", "/v2/graph/visualize/branch.xhtml"),
            Map.entry("/v2/graph/visualize/force", "/v2/graph/visualize/force.xhtml"),
            Map.entry("/v2/thesaurus", "/v2/thesaurus/browse.xhtml"),
            Map.entry("/toolbox/edition", "/toolbox/edition.xhtml"),
            Map.entry("/v2-preview", "/v2-preview/index.xhtml")
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String target = FORWARD_TARGETS.get(request.getServletPath());
        if (target != null) {
            request.getRequestDispatcher(target).forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
