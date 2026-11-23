package fr.cnrs.opentheso.ws.api;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifiants de thésaurus cités par une requête des API REST v1 et v2.
 * Les chemins internes de l'interface (recherche de concepts, compte, etc.) sont ignorés.
 */
final class RestThesaurusTargets {

    private static final Pattern V1_THESAURUS = Pattern.compile("^/openapi/v1/thesaurus/([^/]+)");
    private static final Pattern V1_CONCEPT = Pattern.compile("^/openapi/v1/concept/([^/]+)");
    private static final Pattern V1_GROUP = Pattern.compile("^/openapi/v1/group/([^/]+)");
    private static final Pattern V1_REDIRECT = Pattern.compile("^/openapi/v1/redirect/([^/]+)");
    private static final Pattern V2_WRITE = Pattern.compile("^/api/v2/thesaurus/([^/]+)");
    private static final Pattern V2_MODULE = Pattern.compile("^/openapi/v2/(?:public/)?thesauri/([^/]+)");
    private static final Pattern V1_CONCEPT_SEARCH = Pattern.compile("^/openapi/v1/concept/search/([^/]+)");
    private static final Pattern V2_RECONCILE = Pattern.compile("^/openapi/v2/public/reconciliation/([^/]+)");
    private static final Pattern V2_REDIRECT = Pattern.compile("^/openapi/v2/public/redirect/([^/]+)");
    private static final Pattern V2_PREVIEW = Pattern.compile("^/api/v2/preview/([^/]+)");
    private static final Pattern V2_RECONCILE_LEGACY = Pattern.compile("^/api/v2/([^/]+)/[^/]+/(?:reconcile|suggest/entity)(?:/|$)");
    private static final Pattern LEGACY_DOT = Pattern.compile("^/api/([^/.]+)\\.[^/]+$");

    private static final Set<String> NOT_A_THESAURUS = Set.of(
            "ark", "ark:", "search", "handle", "all", "auth", "preview", "suggest", "propose_properties", "thesaurus", "thesaurus-import");
    private static final String[] QUERY_NAMES = {"theso", "idTheso", "idThesaurus", "thesaurus", "idThesoConcept"};

    private RestThesaurusTargets() {
    }

    static boolean inScope(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return path.startsWith("/openapi/v1")
                || path.startsWith("/openapi/v2")
                || path.startsWith("/api/v2")
                || PublicApiGate.isOpenthesoApi(path);
    }

    static List<String> thesaurusIds(String path, Function<String, String[]> parameters) {
        if (!inScope(path)) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        addPathId(ids, V1_THESAURUS, path);
        addConceptId(ids, path);
        addPathId(ids, V1_GROUP, path);
        addPathId(ids, V1_REDIRECT, path);
        addPathId(ids, V2_WRITE, path);
        addPathId(ids, V2_MODULE, path);
        addPathId(ids, V1_CONCEPT_SEARCH, path);
        addPathId(ids, V2_RECONCILE, path);
        addRedirectId(ids, path);
        addPathId(ids, V2_PREVIEW, path);
        addPathId(ids, V2_RECONCILE_LEGACY, path);
        addLegacyId(ids, path);
        if (parameters != null) {
            for (String name : QUERY_NAMES) {
                String[] values = parameters.apply(name);
                if (values == null) {
                    continue;
                }
                for (String value : values) {
                    addQueryValue(ids, value);
                }
            }
        }
        return List.copyOf(ids);
    }

    private static void addConceptId(Set<String> ids, String path) {
        Matcher matcher = V1_CONCEPT.matcher(path);
        if (!matcher.find()) {
            return;
        }
        String candidate = matcher.group(1);
        if (candidate != null && (candidate.startsWith("ark") || NOT_A_THESAURUS.contains(candidate.toLowerCase(Locale.ROOT)))) {
            return;
        }
        add(ids, candidate);
    }

    private static void addRedirectId(Set<String> ids, String path) {
        Matcher matcher = V2_REDIRECT.matcher(path);
        if (!matcher.find()) {
            return;
        }
        String candidate = matcher.group(1);
        if (candidate != null && candidate.startsWith("ark")) {
            return;
        }
        add(ids, candidate);
    }

    private static void addLegacyId(Set<String> ids, String path) {
        if (path.startsWith("/api/v2") || path.startsWith("/openapi")) {
            return;
        }
        Matcher matcher = LEGACY_DOT.matcher(path);
        if (matcher.find()) {
            add(ids, matcher.group(1));
        }
    }

    private static void addPathId(Set<String> ids, Pattern pattern, String path) {
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            add(ids, matcher.group(1));
        }
    }

    private static void addQueryValue(Set<String> ids, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String token = value.trim();
        int colon = token.indexOf(':');
        int comma = token.indexOf(',');
        if (colon >= 0 && (comma < 0 || colon < comma)) {
            token = token.substring(0, colon);
        } else if (comma >= 0) {
            token = token.substring(0, comma);
        }
        add(ids, token);
    }

    private static void add(Set<String> ids, String value) {
        if (value == null) {
            return;
        }
        String id = value.trim();
        if (id.isEmpty() || NOT_A_THESAURUS.contains(id.toLowerCase(Locale.ROOT))) {
            return;
        }
        ids.add(id);
    }
}
