package fr.cnrs.opentheso.v2.project.api;

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
class ProjectAuthSupportTest {

    @Mock
    private AccountAuthSupport accountAuthSupport;

    @InjectMocks
    private ProjectAuthSupport projectAuthSupport;

    @Test
    void resolveUserId_delegatesToAccountAuthSupport() {
        when(accountAuthSupport.resolveUserId("key-v2", "legacy")).thenReturn(42);

        assertEquals(42, projectAuthSupport.resolveUserId("key-v2", "legacy"));
        verify(accountAuthSupport).resolveUserId("key-v2", "legacy");
    }
}
