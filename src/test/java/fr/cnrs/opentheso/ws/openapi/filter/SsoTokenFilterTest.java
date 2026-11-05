package fr.cnrs.opentheso.ws.openapi.filter;

import fr.cnrs.opentheso.shared.auth.SsoTokenService;
import fr.cnrs.opentheso.shared.session.SessionAuthenticatedUserSource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SsoTokenFilterTest {

    private SsoTokenService ssoTokenService;
    private SsoTokenFilter filter;

    @BeforeEach
    void setUp() {
        ssoTokenService = mock(SsoTokenService.class);
        filter = new SsoTokenFilter(ssoTokenService);
    }

    @Test
    void validToken_logsTheUserInAndOpensTheConcept() throws Exception {
        when(ssoTokenService.validateAndConsumeToken("tok")).thenReturn(42);
        MockHttpServletRequest request = conceptRequest("/idt=th3&idc=42470&ssoToken=tok");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        HttpSession session = request.getSession(false);
        assertEquals(42, session.getAttribute(SessionAuthenticatedUserSource.SESSION_USER_ID_KEY));
        assertEquals(42, session.getAttribute("ssoUserId"));
        assertEquals("/idt=th3&idc=42470", response.getRedirectedUrl());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void invalidToken_doesNotOpenASession() throws Exception {
        when(ssoTokenService.validateAndConsumeToken("bad")).thenReturn(null);
        MockHttpServletRequest request = conceptRequest("/idt=th3&idc=42470&ssoToken=bad");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertNull(request.getSession(false));
        assertEquals("/index.xhtml?ssoError=true", response.getRedirectedUrl());
    }

    @Test
    void formFieldsAreReadAsUtf8() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        AtomicBoolean utf8 = new AtomicBoolean();
        when(request.getCharacterEncoding()).thenAnswer(invocation -> utf8.get() ? StandardCharsets.UTF_8.name() : null);
        doAnswer(invocation -> {
            utf8.set(StandardCharsets.UTF_8.name().equals(invocation.getArgument(0)));
            return null;
        }).when(request).setCharacterEncoding(anyString());
        when(request.getParameter(anyString())).thenAnswer(invocation -> {
            if (!utf8.get()) {
                throw new AssertionError("paramètres lus avant l'encodage UTF-8");
            }
            return null;
        });
        when(request.getRequestURI()).thenReturn("/index.xhtml");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));
    }

    private static MockHttpServletRequest conceptRequest(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setRequestURI(path);
        return request;
    }
}
