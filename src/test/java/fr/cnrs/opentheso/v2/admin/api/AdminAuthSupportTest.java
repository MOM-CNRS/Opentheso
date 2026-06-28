package fr.cnrs.opentheso.v2.admin.api;

import fr.cnrs.opentheso.v2.user.api.AccountAuthSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthSupportTest {

    @Mock
    private AccountAuthSupport accountAuthSupport;

    @InjectMocks
    private AdminAuthSupport adminAuthSupport;

    @Test
    void resolveUserId_delegatesToAccountAuthSupport() {
        when(accountAuthSupport.resolveUserId("key", null)).thenReturn(3);

        assertEquals(3, adminAuthSupport.resolveUserId("key", null));
        verify(accountAuthSupport).resolveUserId("key", null);
    }
}
