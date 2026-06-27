package fr.cnrs.opentheso.v2.setting.service;

import fr.cnrs.opentheso.v2.setting.policy.ThesaurusAccessPolicy;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThesaurusAccessServiceTest {

    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;

    private ThesaurusAccessService thesaurusAccessService;

    @BeforeEach
    void setUp() {
        thesaurusAccessService = new ThesaurusAccessService(thesaurusSettingsQueryRepository);
    }

    @Test
    void canManageThesaurus_grantsSuperAdminWithoutLookup() {
        assertTrue(thesaurusAccessService.canManageThesaurus(1, true, "TH1"));
        verify(thesaurusSettingsQueryRepository, never()).findEffectiveRoleOnThesaurus(1, "TH1");
    }

    @Test
    void canManageThesaurus_grantsThesaurusAdminRole() {
        when(thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(2, "TH1"))
                .thenReturn(Optional.of(ThesaurusAccessPolicy.ROLE_ADMIN));

        assertTrue(thesaurusAccessService.canManageThesaurus(2, false, "TH1"));
    }

    @Test
    void canManageThesaurus_deniesManagerRole() {
        when(thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(3, "TH1"))
                .thenReturn(Optional.of(ThesaurusAccessPolicy.ROLE_MANAGER));

        assertFalse(thesaurusAccessService.canManageThesaurus(3, false, "TH1"));
    }

    @Test
    void canManageThesaurus_deniesWhenNoRoleFound() {
        when(thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(4, "TH1"))
                .thenReturn(Optional.empty());

        assertFalse(thesaurusAccessService.canManageThesaurus(4, false, "TH1"));
    }
}
