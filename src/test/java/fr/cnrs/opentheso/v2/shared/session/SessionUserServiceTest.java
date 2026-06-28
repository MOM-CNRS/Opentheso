package fr.cnrs.opentheso.v2.shared.session;

import fr.cnrs.opentheso.v2.shared.auth.UserCapabilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionUserServiceTest {

    @Mock
    private UserCapabilityService userCapabilityService;

    @InjectMocks
    private SessionUserService sessionUserService;

    @Test
    void load_delegatesToCapabilityService() {
        SessionUser sessionUser = new SessionUser(1, "alice", "a@b.c", false, true, true, true);
        when(userCapabilityService.loadSessionUser(1)).thenReturn(sessionUser);

        assertEquals(sessionUser, sessionUserService.load(1));
    }
}
