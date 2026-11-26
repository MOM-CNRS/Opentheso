package fr.cnrs.opentheso.ws.api;

import java.util.regex.Pattern;

/**
 * Adresses de l'API publique Opentheso et de l'API graphe.
 * Les appels de l'interface ({@code /api/concepts}, {@code /api/account}, la boîte à outils)
 * ne sont pas concernés : les couper casserait l'application.
 */
public final class PublicApiGate {

    // /api/th1.10 (RDF par défaut) et /api/preflabel.fr/{naan}/{ark}.json sont aussi des exports
    private static final Pattern EXPORT = Pattern.compile(
            "^/api/(?:[^/]+\\.[^/]+|[^/]+/[^/]+\\.(?:rdf|json|jsonld|ttl)|preflabel\\.[^/]+/[^/]+/[^/]+\\.json)$");
    private static final Pattern V2_SYNC = Pattern.compile("^/api/v2/thesaurus/[^/]+/sync(?:/.*)?$");
    private static final Pattern REDIRECT = Pattern.compile(
            "^/(?:openapi/v1/redirect|api/theso|api/concept|api/ark:)(?:/.*)?$|^/api/ark:[^/].*");

    private PublicApiGate() {
    }

    public static boolean isOpenthesoApi(String path) {
        String value = normalize(path);
        if (value.startsWith("/openapi/v1") || value.startsWith("/openapi/v2/public")) {
            return true;
        }
        if (value.startsWith("/swagger-ui") || value.startsWith("/v3/api-docs")) {
            return true;
        }
        if ("/api/v2".equals(value) || value.startsWith("/api/v2/")) {
            return !value.startsWith("/api/v2/thesaurus-import");
        }
        if (startsWithSegment(value, "/api/group")
                || startsWithSegment(value, "/api/theso")
                || startsWithSegment(value, "/api/concept")
                || startsWithSegment(value, "/api/search")
                || startsWithSegment(value, "/api/searchwidget")
                || startsWithSegment(value, "/api/searchwidgetbyark")
                || startsWithSegment(value, "/api/autocomplete")
                || startsWithSegment(value, "/api/info")
                || startsWithSegment(value, "/api/expansion")
                || startsWithSegment(value, "/api/all")
                || startsWithSegment(value, "/api/jsonlist")
                || startsWithSegment(value, "/api/getchangesfrom")
                || startsWithSegment(value, "/api/ontome")
                || startsWithSegment(value, "/api/topterm")
                || startsWithSegment(value, "/api/narrower")
                || startsWithSegment(value, "/api/preflabel")
                || startsWithSegment(value, "/api/ark")
                || value.startsWith("/api/doi:")
                || "/api/graph".equals(value)
                || value.startsWith("/api/graph/")) {
            return true;
        }
        return EXPORT.matcher(value).matches();
    }

    /**
     * API V1, obsolète : {@code /openapi/v1}, l'ancienne API {@code /api} et l'ancien {@code /api/v2},
     * comme le groupe « V1 » de la documentation. Restent ouverts : les redirections ARK et permaliens
     * (identifiants pérennes), la synchronisation entre instances et l'import de l'interface.
     */
    public static boolean isApiV1(String path) {
        String value = normalize(path);
        if (REDIRECT.matcher(value).matches() || V2_SYNC.matcher(value).matches()) {
            return false;
        }
        if (startsWithSegment(value, "/openapi/v1") || "/Auth".equals(value)
                || startsWithSegment(value, "/v3/api-docs/V1")) {
            return true;
        }
        return value.startsWith("/api/") && isOpenthesoApi(value);
    }

    public static boolean isGraphApi(String path) {
        String value = normalize(path);
        return "/graphql".equals(value)
                || value.startsWith("/graphql/")
                || "/graphiql".equals(value)
                || "/graphiql.html".equals(value)
                || value.startsWith("/graphiql/");
    }

    private static boolean startsWithSegment(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        int query = path.indexOf('?');
        String value = query >= 0 ? path.substring(0, query) : path;
        int session = value.indexOf(';');
        if (session >= 0) {
            value = value.substring(0, session);
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? "/" : value;
    }
}
