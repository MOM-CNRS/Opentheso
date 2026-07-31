package fr.cnrs.opentheso.v2.rights;

import fr.cnrs.opentheso.v2.shared.auth.UserCapabilityService;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusSettingsQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRightsCacheTest {

    @Mock
    private UserCapabilityService userCapabilityService;
    @Mock
    private ThesaurusSettingsQueryRepository thesaurusSettingsQueryRepository;
    @Mock
    private ProjectAdminQueryRepository projectAdminQueryRepository;

    private UserRightsCache cache;

    @BeforeEach
    void setUp() {
        cache = new UserRightsCache(userCapabilityService, thesaurusSettingsQueryRepository, projectAdminQueryRepository);
        ReflectionTestUtils.setField(cache, "rightsCacheTtl", Duration.ofMinutes(5));
        ReflectionTestUtils.setField(cache, "rightsCacheMaxSize", 1000L);
        cache.initCache();
    }

    @Test
    void getSessionUser_loadsOnceThenServesFromCache() {
        SessionUser sessionUser = new SessionUser(7, "alice", "a@b.c", false, true, true, true);
        when(userCapabilityService.loadSessionUser(7)).thenReturn(sessionUser);

        assertSame(sessionUser, cache.getSessionUser(7));
        assertSame(sessionUser, cache.getSessionUser(7));
        verify(userCapabilityService, times(1)).loadSessionUser(7);
    }

    @Test
    void cachesThesaurusAndProjectRoles() {
        when(userCapabilityService.loadSessionUser(7))
                .thenReturn(new SessionUser(7, "alice", "a@b.c", false, true, true, true));
        when(thesaurusSettingsQueryRepository.findEffectiveRoleOnThesaurus(7, "TH1"))
                .thenReturn(Optional.of(2));
        when(projectAdminQueryRepository.findCallerRoleOnProject(7, 3))
                .thenReturn(Optional.of(2));

        assertEquals(Optional.of(2), cache.getEffectiveRoleOnThesaurus(7, "TH1"));
        assertEquals(Optional.of(2), cache.getEffectiveRoleOnThesaurus(7, "TH1"));
        assertEquals(Optional.of(2), cache.getRoleOnProject(7, 3));
        assertEquals(Optional.of(2), cache.getRoleOnProject(7, 3));

        verify(thesaurusSettingsQueryRepository, times(1)).findEffectiveRoleOnThesaurus(7, "TH1");
        verify(projectAdminQueryRepository, times(1)).findCallerRoleOnProject(7, 3);
    }

    @Test
    void invalidate_forcesReload() {
        SessionUser first = new SessionUser(7, "alice", "a@b.c", false, true, true, true);
        SessionUser second = new SessionUser(7, "alice", "a@b.c", true, true, true, true);
        when(userCapabilityService.loadSessionUser(7)).thenReturn(first, second);

        assertSame(first, cache.getSessionUser(7));
        cache.invalidate(7);
        assertSame(second, cache.getSessionUser(7));
        verify(userCapabilityService, times(2)).loadSessionUser(7);
    }
}
