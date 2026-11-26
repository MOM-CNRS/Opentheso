package fr.cnrs.opentheso.ws.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicApiGateTest {

    @Test
    void openthesoApiCoversThePublicSurfaces() {
        assertTrue(PublicApiGate.isOpenthesoApi("/openapi/v1/ping"));
        assertTrue(PublicApiGate.isOpenthesoApi("/openapi/v2/public/thesauri"));
        assertTrue(PublicApiGate.isOpenthesoApi("/api/v2/thesaurus/th60/concepts"));
        assertTrue(PublicApiGate.isOpenthesoApi("/api/search"));
        assertTrue(PublicApiGate.isOpenthesoApi("/api/th60.10.json"));
        assertTrue(PublicApiGate.isOpenthesoApi("/swagger-ui/index.html"));
        assertFalse(PublicApiGate.isOpenthesoApi("/api/v2/thesaurus-import/progress"));
        assertFalse(PublicApiGate.isOpenthesoApi("/api/concepts/search"));
        assertFalse(PublicApiGate.isOpenthesoApi("/api/account/tree-status"));
        assertFalse(PublicApiGate.isOpenthesoApi("/api/graph-neighborhood"));
        assertFalse(PublicApiGate.isOpenthesoApi("/openapi/v2/thesauri/th60/concepts"));
        assertFalse(PublicApiGate.isOpenthesoApi("/graphql"));
    }

    @Test
    void disabledBodyExplainsWhy() {
        assertTrue(PublicApiGateFilter.disabledBody(true).contains("L'API Opentheso est désactivée"));
        assertTrue(PublicApiGateFilter.disabledBody(false).contains("L'API graphe est désactivée"));
        assertFalse(PublicApiGateFilter.disabledBody(true).contains("not-found"));
    }

    @Test
    void apiV1MatchesTheDocumentedV1Group() {
        for (String path : new String[]{
                "/openapi/v1/thesaurus/th60", "/openapi/v1/ping", "/Auth", "/v3/api-docs/V1",
                "/api/th60.10.json", "/api/th60.10", "/api/preflabel.fr/26678/x.json", "/api/26678/x.rdf",
                "/api/search", "/api/all/theso", "/api/group/26678/g1", "/api/v2/th60/fr/reconcile",
                "/api/v2/thesaurus/th60/concepts", "/api/v2/preview/th60/1"}) {
            assertTrue(PublicApiGate.isApiV1(path), path);
        }
        for (String path : new String[]{
                "/openapi/v2/public/thesauri", "/openapi/v2/thesauri/th60/settings/preferences", "/v3/api-docs/V2",
                "/swagger-ui/index.html", "/api/ark:/26678/x", "/api/ark:26678/x", "/openapi/v1/redirect/th60/1",
                "/api/theso/pactols", "/api/concept/th60.1", "/api/v2/thesaurus/th60/sync/concepts",
                "/api/v2/thesaurus-import/progress", "/api/concepts/search", "/api/graph-neighborhood",
                "/api/maintenance/sitemap.xml", "/graphql"}) {
            assertFalse(PublicApiGate.isApiV1(path), path);
        }
    }

    @Test
    void graphApiIsSeparate() {
        assertTrue(PublicApiGate.isGraphApi("/graphql"));
        assertTrue(PublicApiGate.isGraphApi("/graphiql.html"));
        assertFalse(PublicApiGate.isGraphApi("/api/graph"));
        assertFalse(PublicApiGate.isOpenthesoApi("/graphiql.html"));
    }
}
