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
        assertFalse(PublicApiGate.isOpenthesoApi("/api/graph-globe"));
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
    void graphApiIsSeparate() {
        assertTrue(PublicApiGate.isGraphApi("/graphql"));
        assertTrue(PublicApiGate.isGraphApi("/graphiql.html"));
        assertFalse(PublicApiGate.isGraphApi("/api/graph"));
        assertFalse(PublicApiGate.isOpenthesoApi("/graphiql.html"));
    }
}
