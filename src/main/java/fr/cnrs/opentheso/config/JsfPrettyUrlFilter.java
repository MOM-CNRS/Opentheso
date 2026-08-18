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

    private static final String V2_PREVIEW_PREFIX = "/v2-preview";

    private static final Map<String, String> FORWARD_TARGETS = Map.ofEntries(
            Map.entry("/", "/index.xhtml"),
            Map.entry("/reset-password", "/reset-password.xhtml"),
            Map.entry("/profile", "/profile/myAccount.xhtml"),
            Map.entry("/candidat", "/candidat/candidat.xhtml"),
            Map.entry("/toolbox/edition", "/toolbox/edition.xhtml"),
            Map.entry("/v2", "/v2/index.xhtml"),
            Map.entry(V2_PREVIEW_PREFIX, "/v2/index.xhtml")
    );

    /** Anciennes pages plates `/v2-preview/*.xhtml` → arborescence `/v2/`. */
    private static final Map<String, String> V2_PREVIEW_PAGES = Map.ofEntries(
            Map.entry("/index.xhtml", "/v2/index.xhtml"),
            Map.entry("/preference.xhtml", "/v2/setting/preference.xhtml"),
            Map.entry("/identifiants.xhtml", "/v2/setting/identifiants.xhtml"),
            Map.entry("/corpus.xhtml", "/v2/setting/corpus.xhtml"),
            Map.entry("/parametres.xhtml", "/v2/setting/parametres.xhtml"),
            Map.entry("/statistiques.xhtml", "/v2/toolbox/statistiques.xhtml"),
            Map.entry("/atelier.xhtml", "/v2/toolbox/atelier.xhtml"),
            Map.entry("/maintenance.xhtml", "/v2/toolbox/maintenance.xhtml"),
            Map.entry("/actions-lot.xhtml", "/v2/toolbox/actions-lot.xhtml"),
            Map.entry("/candidats.xhtml", "/v2/candidat/candidats.xhtml"),
            Map.entry("/graphe.xhtml", "/v2/graph/graphe.xhtml"),
            Map.entry("/compte.xhtml", "/v2/user/compte.xhtml"),
            Map.entry("/projets.xhtml", "/v2/project/projets.xhtml"),
            Map.entry("/admin-utilisateurs.xhtml", "/v2/admin/utilisateurs.xhtml"),
            Map.entry("/admin-projets.xhtml", "/v2/admin/projets.xhtml"),
            Map.entry("/admin-thesauri.xhtml", "/v2/admin/thesauri.xhtml"),
            Map.entry("/consultation.xhtml", "/v2/thesaurus/consultation.xhtml"),
            Map.entry("/target.xhtml", "/v2/prototype/target.xhtml")
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String target = resolveForwardTarget(request.getServletPath());
        if (target != null) {
            request.getRequestDispatcher(target).forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    static String resolveForwardTarget(String servletPath) {
        if (servletPath == null) {
            return null;
        }
        String target = FORWARD_TARGETS.get(servletPath);
        if (target != null) {
            return target;
        }
        if (servletPath.startsWith(V2_PREVIEW_PREFIX + "/")) {
            return V2_PREVIEW_PAGES.get(servletPath.substring(V2_PREVIEW_PREFIX.length()));
        }
        return null;
    }
}
