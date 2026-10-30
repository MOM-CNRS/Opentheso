package fr.cnrs.opentheso.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
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
        String path = conceptRequestPath(request);
        Map<String, String[]> pathParams = conceptPathParameters(path);
        if (pathParams != null) {
            request.getRequestDispatcher("/index.xhtml")
                    .forward(new ConceptPathRequest(request, pathParams), response);
            return;
        }
        boolean conceptHome = hasText(request.getParameter("idt")) || hasText(request.getParameter("idc"));
        if (conceptHome && isSafeMethod(request) && (isIndexAlias(path) || "/".equals(path))) {
            String conceptPath = conceptBrowserPathFromQuery(request.getQueryString());
            if (conceptPath != null) {
                response.sendRedirect(request.getContextPath() + conceptPath);
                return;
            }
        }
        String target = resolveForwardTarget(path, conceptHome);
        if (target != null) {
            request.getRequestDispatcher(target).forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * {@code /idt=th217&idc=1895753} à partir des valeurs brutes.
     * {@code null} si aucun identifiant n'est fourni.
     */
    public static String conceptBrowserPath(String idt, String idc) {
        Map<String, String[]> params = new LinkedHashMap<>();
        if (hasText(idt)) {
            params.put("idt", new String[]{idt.trim()});
        }
        if (hasText(idc)) {
            params.put("idc", new String[]{idc.trim()});
        }
        return conceptBrowserPath(params);
    }

    /** Reprend une query {@code idt=…&idc=…} et la réécrit en chemin. */
    public static String conceptBrowserPathFromQuery(String query) {
        return conceptBrowserPath(parseQuery(query));
    }

    static String resolveForwardTarget(String servletPath) {
        return resolveForwardTarget(servletPath, false);
    }

    /**
     * {@code /idt=} et {@code /idc=} ouvrent l'accueil thésaurus
     * ({@code index.xhtml}). {@code /} sans ces paramètres reste le catalogue.
     */
    static String resolveForwardTarget(String servletPath, boolean conceptHome) {
        if (servletPath == null) {
            return null;
        }
        String path = normalize(servletPath);
        if (conceptPathParameters(path) != null || ("/".equals(path) && conceptHome)) {
            return "/index.xhtml";
        }
        return FORWARD_TARGETS.get(path);
    }

    static Map<String, String[]> conceptPathParameters(String path) {
        if (path == null || !path.startsWith("/")) {
            return null;
        }
        String raw = path.substring(1);
        if (raw.isEmpty() || raw.indexOf('/') >= 0) {
            return null;
        }
        if (!raw.startsWith("idt=") && !raw.startsWith("idc=")) {
            return null;
        }
        Map<String, List<String>> parsed = new LinkedHashMap<>();
        for (String part : raw.split("&", -1)) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                return null;
            }
            String name = decodePath(part.substring(0, eq));
            String value = decodePath(part.substring(eq + 1));
            if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                return null;
            }
            parsed.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        }
        if (!hasText(first(parsed.get("idt"))) && !hasText(first(parsed.get("idc")))) {
            return null;
        }
        Map<String, String[]> params = new LinkedHashMap<>();
        parsed.forEach((name, values) -> params.put(name, values.toArray(String[]::new)));
        return params;
    }

    private static String conceptBrowserPath(Map<String, String[]> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        String idt = first(params.get("idt"));
        String idc = first(params.get("idc"));
        if (!hasText(idt) && !hasText(idc)) {
            return null;
        }
        StringBuilder path = new StringBuilder();
        if (hasText(idt)) {
            appendPathParam(path, "idt", idt);
        }
        if (hasText(idc)) {
            appendPathParam(path, "idc", idc);
        }
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if ("idt".equals(entry.getKey()) || "idc".equals(entry.getKey())) {
                continue;
            }
            String value = first(entry.getValue());
            if (hasText(value) && entry.getKey().matches("[A-Za-z_][A-Za-z0-9_]*")) {
                appendPathParam(path, entry.getKey(), value);
            }
        }
        return path.toString();
    }

    private static void appendPathParam(StringBuilder path, String name, String value) {
        path.append(path.isEmpty() ? '/' : '&').append(name).append('=').append(encodePathParam(value));
    }

    private static String encodePathParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Map<String, String[]> parseQuery(String query) {
        Map<String, List<String>> parsed = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return Map.of();
        }
        for (String part : query.split("&", -1)) {
            if (part.isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            String name = decode(eq >= 0 ? part.substring(0, eq) : part);
            String value = decode(eq >= 0 ? part.substring(eq + 1) : "");
            if (!name.isEmpty()) {
                parsed.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
            }
        }
        Map<String, String[]> params = new LinkedHashMap<>();
        parsed.forEach((name, values) -> params.put(name, values.toArray(String[]::new)));
        return params;
    }

    private static String conceptRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String path = servletPath == null ? "" : normalize(servletPath);
        if (conceptPathParameters(path) != null) {
            return path;
        }
        String uri = request.getRequestURI();
        String context = request.getContextPath() == null ? "" : request.getContextPath();
        if (uri != null && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        if (uri == null || uri.isEmpty()) {
            return path.isEmpty() ? "/" : path;
        }
        return normalize(decodePath(uri));
    }

    private static String first(String[] values) {
        return values == null || values.length == 0 ? null : values[0];
    }

    private static String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static String decodePath(String value) {
        return decode(value.replace("+", "%2B"));
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private static boolean isIndexAlias(String path) {
        return "/index".equals(path) || "/index.xhtml".equals(path);
    }

    private static boolean isSafeMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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

    /** Expose les paramètres portés par {@code /idt=…&idc=…} comme une query. */
    private static final class ConceptPathRequest extends HttpServletRequestWrapper {

        private final Map<String, String[]> pathParams;

        private ConceptPathRequest(HttpServletRequest request, Map<String, String[]> pathParams) {
            super(request);
            this.pathParams = pathParams;
        }

        @Override
        public String getParameter(String name) {
            String[] values = pathParams.get(name);
            if (values != null && values.length > 0) {
                return values[0];
            }
            return super.getParameter(name);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = pathParams.get(name);
            return values != null ? values.clone() : super.getParameterValues(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> merged = new LinkedHashMap<>(super.getParameterMap());
            pathParams.forEach((name, values) -> merged.put(name, values.clone()));
            return Collections.unmodifiableMap(merged);
        }

        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(getParameterMap().keySet());
        }
    }
}
