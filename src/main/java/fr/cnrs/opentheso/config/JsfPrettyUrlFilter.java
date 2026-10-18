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

    private static final String FORMER_UI_PREFIX = "/v2";
    private static final String FORMER_PREVIEW_PREFIX = "/v2-preview";

    private static final Map<String, String> FORWARD_TARGETS = Map.ofEntries(
            Map.entry("/", "/thesauri.xhtml"),
            Map.entry("/index", "/index.xhtml"),
            Map.entry("/index.xhtml", "/index.xhtml"),
            Map.entry("/reset-password", "/reset-password.xhtml"),
            Map.entry("/profile", "/user/compte.xhtml"),
            Map.entry("/candidat", "/candidat/candidats.xhtml"),
            Map.entry("/toolbox/edition", "/toolbox/atelier.xhtml"),
            Map.entry("/thesauri", "/thesauri.xhtml"),
            Map.entry("/admin/instance", "/admin/instance.xhtml"),
            Map.entry("/admin/projets", "/admin/projets.xhtml"),
            Map.entry("/admin/thesauri", "/admin/thesauri.xhtml"),
            Map.entry("/admin/utilisateurs", "/admin/utilisateurs.xhtml"),
            Map.entry("/setting/preference", "/setting/preference.xhtml"),
            Map.entry("/setting/identifiants", "/setting/identifiants.xhtml"),
            Map.entry("/setting/corpus", "/setting/corpus.xhtml"),
            Map.entry("/setting/parametres", "/setting/parametres.xhtml"),
            Map.entry("/project/projets", "/project/projets.xhtml"),
            Map.entry("/user/compte", "/user/compte.xhtml"),
            Map.entry("/user/preference", "/user/preference.xhtml"),
            Map.entry("/proposition/propositions", "/proposition/propositions.xhtml"),
            Map.entry("/candidat/candidats", "/candidat/candidats.xhtml"),
            Map.entry("/toolbox/atelier", "/toolbox/atelier.xhtml"),
            Map.entry("/toolbox/maintenance", "/toolbox/maintenance.xhtml"),
            Map.entry("/toolbox/statistiques", "/toolbox/statistiques.xhtml"),
            Map.entry("/toolbox/synchronisation", "/toolbox/synchronisation.xhtml"),
            Map.entry("/toolbox/portail", "/toolbox/portail.xhtml"),
            Map.entry("/toolbox/actions-lot", "/toolbox/actions-lot.xhtml"),
            Map.entry("/graph/graphe", "/graph/graphe.xhtml"),
            Map.entry("/thesaurus/consultation", "/thesaurus/consultation.xhtml"),
            Map.entry("/preference.xhtml", "/setting/preference.xhtml"),
            Map.entry("/identifiants.xhtml", "/setting/identifiants.xhtml"),
            Map.entry("/corpus.xhtml", "/setting/corpus.xhtml"),
            Map.entry("/parametres.xhtml", "/setting/parametres.xhtml"),
            Map.entry("/statistiques.xhtml", "/toolbox/statistiques.xhtml"),
            Map.entry("/atelier.xhtml", "/toolbox/atelier.xhtml"),
            Map.entry("/maintenance.xhtml", "/toolbox/maintenance.xhtml"),
            Map.entry("/synchronisation.xhtml", "/toolbox/synchronisation.xhtml"),
            Map.entry("/portail.xhtml", "/toolbox/portail.xhtml"),
            Map.entry("/actions-lot.xhtml", "/toolbox/actions-lot.xhtml"),
            Map.entry("/candidats.xhtml", "/candidat/candidats.xhtml"),
            Map.entry("/graphe.xhtml", "/graph/graphe.xhtml"),
            Map.entry("/compte.xhtml", "/user/compte.xhtml"),
            Map.entry("/preference-user.xhtml", "/user/preference.xhtml"),
            Map.entry("/projets.xhtml", "/project/projets.xhtml"),
            Map.entry("/admin-utilisateurs.xhtml", "/admin/utilisateurs.xhtml"),
            Map.entry("/admin-projets.xhtml", "/admin/projets.xhtml"),
            Map.entry("/admin-thesauri.xhtml", "/admin/thesauri.xhtml"),
            Map.entry("/consultation.xhtml", "/thesaurus/consultation.xhtml"),
            Map.entry("/target.xhtml", "/prototype/target.xhtml")
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
        return FORWARD_TARGETS.get(normalize(servletPath));
    }

    /**
     * Les adresses {@code /v2/...} et {@code /v2-preview/...} restent acceptées
     * le temps que les liens déjà publiés basculent.
     */
    private static String normalize(String servletPath) {
        if (FORMER_UI_PREFIX.equals(servletPath)) {
            return "/";
        }
        if (servletPath.startsWith(FORMER_UI_PREFIX + "/")) {
            return servletPath.substring(FORMER_UI_PREFIX.length());
        }
        if (FORMER_PREVIEW_PREFIX.equals(servletPath)) {
            return "/";
        }
        if (servletPath.startsWith(FORMER_PREVIEW_PREFIX + "/")) {
            return servletPath.substring(FORMER_PREVIEW_PREFIX.length());
        }
        return servletPath;
    }
}
