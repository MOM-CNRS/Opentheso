package fr.cnrs.opentheso.v2.shared.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationUriServiceTest {

    private final ApplicationUriService service = new ApplicationUriService();

    @Test
    void resolveApplicationBaseUrl_returnsEmptyOutsideFacesContext() {
        assertEquals("", service.resolveApplicationBaseUrl());
    }

    @Test
    void resolveApplicationRootUrl_returnsEmptyWhenBaseUrlEmpty() {
        assertEquals("", service.resolveApplicationRootUrl());
    }

    @Test
    void resolveUrl_returnsEmptyWhenBaseUrlEmpty() {
        assertEquals("", service.resolveUrl("/swagger-ui/index.html"));
        assertEquals("", service.resolveSwaggerUrl());
        assertEquals("", service.resolveOpenApiUrl());
        assertEquals("", service.resolveGraphQlUrl());
        assertEquals("", service.resolveGraphiqlUrl());
    }
}
