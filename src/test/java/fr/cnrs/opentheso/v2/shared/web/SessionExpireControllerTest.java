package fr.cnrs.opentheso.v2.shared.web;

import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionExpireControllerTest {

    @Mock
    private SessionLifecycleService sessionLifecycleService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private SessionExpireController controller;

    @Test
    void expire_delegatesToLifecycleService() throws Exception {
        controller.expire(request, response);

        verify(sessionLifecycleService).expireAndRedirect(request, response);
    }

    @Test
    void keepAlive_returnsNoContentWhenSessionExists() {
        when(sessionLifecycleService.touch(request)).thenReturn(true);

        var response = controller.keepAlive(request);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void keepAlive_returnsUnauthorizedWhenSessionMissing() {
        when(sessionLifecycleService.touch(request)).thenReturn(false);

        var response = controller.keepAlive(request);

        assertEquals(401, response.getStatusCode().value());
    }
}
