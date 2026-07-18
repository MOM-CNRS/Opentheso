package fr.cnrs.opentheso.v2.publicapi.resolver.api;

import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.resolver.service.ArkRedirectService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectPublicControllerTest {

    @Mock
    private ArkRedirectService arkRedirectService;
    @Mock
    private HttpServletRequest request;

    private RedirectPublicController controller;

    @BeforeEach
    void setUp() {
        controller = new RedirectPublicController(arkRedirectService);
    }

    @Test
    void redirectFromArk_returns307WithResolvedLocation() throws Exception {
        when(arkRedirectService.buildRedirectUrl("naan", "ark1")).thenReturn(Optional.of("https://site/?idc=C1&idt=TH1"));

        var response = controller.redirectFromArk("naan", "ark1");

        assertEquals(307, response.getStatusCode().value());
        assertEquals("https://site/?idc=C1&idt=TH1", response.getHeaders().getLocation().toString());
    }

    @Test
    void redirectFromArk_throwsWhenUnresolved() {
        when(arkRedirectService.buildRedirectUrl("naan", "ark9")).thenReturn(Optional.empty());

        assertThrows(PublicResourceNotFoundException.class, () -> controller.redirectFromArk("naan", "ark9"));
    }

    @Test
    void redirectToConcept_buildsRewrittenUrl() throws Exception {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host/openapi/v2/public/redirect/TH1/C1"));

        var response = controller.redirectToConcept("TH1", "C1", request);

        assertEquals(307, response.getStatusCode().value());
        assertEquals("http://host/?idc=C1&idt=TH1", response.getHeaders().getLocation().toString());
    }

    @Test
    void redirectToThesaurus_buildsRewrittenUrl() throws Exception {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host/openapi/v2/public/redirect/TH1"));

        var response = controller.redirectToThesaurus("TH1", request);

        assertEquals(307, response.getStatusCode().value());
        assertEquals("http://host/?idt=TH1", response.getHeaders().getLocation().toString());
    }
}
