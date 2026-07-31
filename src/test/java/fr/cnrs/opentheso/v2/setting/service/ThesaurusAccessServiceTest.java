package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusAccessServiceTest {

    @Mock
    private RightsService rightsService;

    private ThesaurusAccessService thesaurusAccessService;

    @BeforeEach
    void setUp() {
        thesaurusAccessService = new ThesaurusAccessService(rightsService);
    }

    @Test
    void canManageThesaurus_grantsSuperAdminWithoutLookup() {
        assertTrue(thesaurusAccessService.canManageThesaurus(1, true, "TH1"));
        verify(rightsService, never()).can(1, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus("TH1"));
    }

    @Test
    void canManageThesaurus_delegatesToRightsService() {
        when(rightsService.can(2, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus("TH1"))).thenReturn(true);
        assertTrue(thesaurusAccessService.canManageThesaurus(2, false, "TH1"));

        when(rightsService.can(3, Permission.MANAGE_THESAURUS, AuthTarget.thesaurus("TH1"))).thenReturn(false);
        assertFalse(thesaurusAccessService.canManageThesaurus(3, false, "TH1"));
    }
}
