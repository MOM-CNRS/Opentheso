package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.user.model.ProfileWithRoles;
import fr.cnrs.opentheso.v2.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserRoleOverviewService userRoleOverviewService;

    private UserLookupService userLookupService;
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userLookupService = new UserLookupService(userProfileRepository);
        userProfileService = new UserProfileService(
                userProfileRepository,
                userLookupService,
                userRoleOverviewService
        );
    }

    @Test
    void getProfileWithRoles_loadsProfileAndRolesInOneFlow() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, "encrypted-key");
        List<ProjectRoleOverview> roles = List.of();

        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userRoleOverviewService.loadProjectRoles(42, false)).thenReturn(roles);

        ProfileWithRoles result = userProfileService.getProfileWithRoles(42);

        assertEquals(42, result.profile().id());
        assertEquals(roles, result.projectRoles());
    }

    @Test
    void saveEntity_mapsSavedEntity() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, null);
        when(userProfileRepository.save(entity)).thenReturn(entity);

        UserProfile profile = userProfileService.saveEntity(entity);

        assertEquals("alice", profile.username());
        verify(userProfileRepository).save(entity);
    }

    @Test
    void getProfile_returnsMappedProfileWhenUserExists() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, "encrypted-key");

        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));

        UserProfile profile = userProfileService.getProfile(42);

        assertEquals(42, profile.id());
        assertEquals("alice", profile.username());
        assertEquals("alice@example.com", profile.email());
        assertTrue(profile.alertMail());
        assertFalse(profile.superAdmin());
        assertTrue(profile.keyNeverExpire());
        assertTrue(profile.hasApiKey());
    }

    @Test
    void getProfile_throwsWhenUserMissing() {
        when(userProfileRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userProfileService.getProfile(99));
    }

    @Test
    void updateUsername_persistsTrimmedUsername() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, null);
        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userProfileRepository.existsByUsernameIgnoreCaseExcludingId("bob", 42)).thenReturn(false);
        when(userProfileRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = userProfileService.updateUsername(42, "  bob  ");

        assertEquals("bob", profile.username());
        assertEquals("bob", entity.getUsername());
    }

    @Test
    void updateUsername_rejectsBlankValue() {
        assertThrows(InvalidProfileDataException.class, () -> userProfileService.updateUsername(42, "  "));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateUsername_rejectsDuplicate() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, null);
        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userProfileRepository.existsByUsernameIgnoreCaseExcludingId("bob", 42)).thenReturn(true);

        assertThrows(InvalidProfileDataException.class, () -> userProfileService.updateUsername(42, "bob"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateIdentity_persistsUsernameAndEmailInOneSave() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, null);
        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userProfileRepository.existsByUsernameIgnoreCaseExcludingId("bob", 42)).thenReturn(false);
        when(userProfileRepository.existsByMailIgnoreCaseExcludingId("bob@example.com", 42)).thenReturn(false);
        when(userProfileRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = userProfileService.updateIdentity(42, "  bob  ", " bob@example.com ");

        assertEquals("bob", profile.username());
        assertEquals("bob@example.com", profile.email());
        assertEquals("bob", entity.getUsername());
        assertEquals("bob@example.com", entity.getMail());
        verify(userProfileRepository).save(entity);
    }

    @Test
    void updateIdentity_rejectsBlankUsernameWithoutSaving() {
        assertThrows(InvalidProfileDataException.class,
                () -> userProfileService.updateIdentity(42, "  ", "alice@example.com"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateIdentity_rejectsInvalidEmailWithoutSaving() {
        assertThrows(InvalidProfileDataException.class,
                () -> userProfileService.updateIdentity(42, "alice", "invalid"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateIdentity_rejectsDuplicateUsername() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, null);
        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userProfileRepository.existsByUsernameIgnoreCaseExcludingId("bob", 42)).thenReturn(true);

        assertThrows(InvalidProfileDataException.class,
                () -> userProfileService.updateIdentity(42, "bob", "alice@example.com"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateEmail_persistsValidEmail() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, null);
        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userProfileRepository.existsByMailIgnoreCaseExcludingId("new@example.com", 42)).thenReturn(false);
        when(userProfileRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = userProfileService.updateEmail(42, " new@example.com ");

        assertEquals("new@example.com", profile.email());
        assertEquals("new@example.com", entity.getMail());
    }

    @Test
    void updateEmail_rejectsInvalidFormat() {
        assertThrows(InvalidProfileDataException.class, () -> userProfileService.updateEmail(42, "invalid"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateEmail_rejectsDuplicate() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", true, false, true, null, null);
        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userProfileRepository.existsByMailIgnoreCaseExcludingId("bob@example.com", 42)).thenReturn(true);

        assertThrows(InvalidProfileDataException.class,
                () -> userProfileService.updateEmail(42, "bob@example.com"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void updateAlertMail_persistsValue() {
        UserEntity entity = buildEntity(42, "alice", "alice@example.com", false, false, true, null, null);
        when(userProfileRepository.findById(42)).thenReturn(Optional.of(entity));
        when(userProfileRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile profile = userProfileService.updateAlertMail(42, true);

        assertTrue(profile.alertMail());
        assertTrue(entity.getAlertMail());
    }

    private static UserEntity buildEntity(
            int id,
            String username,
            String mail,
            boolean alertMail,
            boolean superAdmin,
            boolean keyNeverExpire,
            LocalDate keyExpiresAt,
            String apiKey
    ) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setMail(mail);
        entity.setAlertMail(alertMail);
        entity.setSuperAdmin(superAdmin);
        entity.setKeyNeverExpire(keyNeverExpire);
        entity.setKeyExpiresAt(keyExpiresAt);
        entity.setApiKey(apiKey);
        return entity;
    }
}
