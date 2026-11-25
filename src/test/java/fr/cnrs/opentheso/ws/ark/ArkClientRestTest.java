package fr.cnrs.opentheso.ws.ark;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArkClientRestTest {

    @Test
    void arkEndpoint_addsHttpsWhenTheSchemeIsMissing() {
        var uri = ArkClientRest.arkEndpoint("ark.mom.fr", "/rest/login/username=a&password=b&naan=1");

        assertEquals("https", uri.getScheme());
        assertEquals("ark.mom.fr", uri.getHost());
        assertEquals("/rest/login/username=a&password=b&naan=1", uri.getRawPath());
    }

    @Test
    void arkEndpoint_keepsAnExplicitHttpSchemeAndDropsTheTrailingSlash() {
        var uri = ArkClientRest.arkEndpoint("http://localhost:8080/", "/rest/v1/ark/add");

        assertEquals("http", uri.getScheme());
        assertEquals("localhost", uri.getHost());
        assertEquals(8080, uri.getPort());
        assertEquals("/rest/v1/ark/add", uri.getPath());
    }

    @Test
    void arkEndpoint_rejectsABlankServer() {
        assertThrows(IllegalArgumentException.class, () -> ArkClientRest.arkEndpoint("  ", "/rest/login"));
    }
}
