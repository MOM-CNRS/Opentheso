package fr.cnrs.opentheso.listeners;

import fr.cnrs.opentheso.services.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyInterceptorTest {

    @Mock
    private ApiKeyService apiKeyService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    void preHandle_allowsPreferenceSaveWithoutApiKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/account/tree-status");
        ApiKeyInterceptor interceptor = new ApiKeyInterceptor(apiKeyService);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        verifyNoInteractions(apiKeyService);
    }
}
