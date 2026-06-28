package fr.cnrs.opentheso.v2.user.api;

import fr.cnrs.opentheso.v2.shared.auth.ApiKeyAuthenticationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAuthSupportTest {

    @Mock
    private ApiKeyAuthenticationService apiKeyAuthenticationService;

    @InjectMocks
    private AccountAuthSupport accountAuthSupport;

    @Test
    void resolveUserId_delegatesToSharedService() {
        when(apiKeyAuthenticationService.resolveUserId("key-v2", null)).thenReturn(10);

        assertEquals(10, accountAuthSupport.resolveUserId("key-v2", null));
        verify(apiKeyAuthenticationService).resolveUserId("key-v2", null);
    }
}
