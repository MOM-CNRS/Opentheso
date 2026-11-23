package fr.cnrs.opentheso.ws.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestThesaurusTargetsTest {

    @Test
    void readsThesaurusIdFromRestPaths() {
        assertEquals(java.util.List.of("th60"), ids("/openapi/v1/thesaurus/th60"));
        assertEquals(java.util.List.of("th60"), ids("/openapi/v1/thesaurus/th60/topconcept"));
        assertEquals(java.util.List.of("th60"), ids("/openapi/v1/concept/th60"));
        assertEquals(java.util.List.of("th60"), ids("/openapi/v1/concept/search/th60/aaron"));
        assertEquals(java.util.List.of("th60"), ids("/openapi/v1/group/th60"));
        assertEquals(java.util.List.of("th17"), ids("/openapi/v2/public/thesauri/th17/concepts"));
        assertEquals(java.util.List.of("th17"), ids("/openapi/v2/thesauri/th17/settings"));
        assertEquals(java.util.List.of("TH_1"), ids("/api/v2/thesaurus/TH_1/concepts"));
        assertEquals(java.util.List.of("th60"), ids("/api/v2/th60/fr/reconcile"));
        assertEquals(java.util.List.of("th60"), ids("/api/v2/preview/th60/10"));
        assertEquals(java.util.List.of("th60"), ids("/openapi/v2/public/reconciliation/th60/fr/manifest"));
        assertEquals(java.util.List.of("th60"), ids("/openapi/v2/public/redirect/th60/10"));
        assertEquals(java.util.List.of("th60"), ids("/api/th60.10.json"));
    }

    @Test
    void ignoresPathsThatDoNotNameAThesaurus() {
        assertTrue(ids("/openapi/v1/thesaurus").isEmpty());
        assertTrue(ids("/openapi/v1/ping").isEmpty());
        assertTrue(ids("/openapi/v1/concept/search").isEmpty());
        assertTrue(ids("/openapi/v1/concept/ark:/26678/pcrt").isEmpty());
        assertTrue(ids("/openapi/v1/concept/handle/123/abc").isEmpty());
        assertTrue(ids("/openapi/v2/public/redirect/ark:/26678/pcrt").isEmpty());
        assertTrue(ids("/api/v2/auth").isEmpty());
        assertTrue(ids("/api/v2/suggest/properties").isEmpty());
        assertTrue(ids("/api/concepts/search").isEmpty());
        assertTrue(ids("/api/account").isEmpty());
        assertTrue(ids("/thesauri").isEmpty());
    }

    @Test
    void readsThesaurusIdFromQuery() {
        assertEquals(java.util.List.of("TH_1"), ids("/api/search", Map.of("theso", new String[]{"TH_1"})));
        assertEquals(java.util.List.of("th3"), ids("/openapi/v1/graph/getData", Map.of("idThesoConcept", new String[]{"th3,4"})));
        assertEquals(java.util.List.of("th3"), ids("/openapi/v2/public/graph", Map.of("idThesoConcept", new String[]{"th3:4"})));
        assertTrue(ids("/api/info/list", Map.of("theso", new String[]{"all"})).isEmpty());
    }

    private static java.util.List<String> ids(String path) {
        return ids(path, Map.of());
    }

    private static java.util.List<String> ids(String path, Map<String, String[]> parameters) {
        return RestThesaurusTargets.thesaurusIds(path, parameters::get);
    }
}
