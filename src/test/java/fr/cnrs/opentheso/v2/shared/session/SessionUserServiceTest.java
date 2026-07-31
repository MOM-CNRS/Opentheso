package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.rights.RightsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionUserServiceTest {

    @Mock
    private RightsService rightsService;

    @InjectMocks
    private SessionUserService sessionUserService;

    @Test
    void load_delegatesToRightsService() {
        SessionUser sessionUser = new SessionUser(1, "alice", "a@b.c", false, true, true, true);
        when(rightsService.capabilities(1)).thenReturn(sessionUser);

        assertEquals(sessionUser, sessionUserService.load(1));
    }

    @Test
    void invalidate_delegatesToRightsService() {
        sessionUserService.invalidate(7);
        verify(rightsService).invalidate(7);
    }
}
