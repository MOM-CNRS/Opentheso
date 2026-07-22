package fr.cnrs.opentheso.v2.shared.web;

import fr.cnrs.opentheso.v2.shared.session.SessionLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

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
}
