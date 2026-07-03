package fr.cnrs.opentheso.v2.user.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.user.exception.ApiKeyRegenerationException;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.model.ApiKeyGenerationResult;
import fr.cnrs.opentheso.v2.user.model.ProfileWithRoles;
import fr.cnrs.opentheso.v2.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserApiKeyService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyAccountBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean localeBean;
    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserApiKeyService userApiKeyService;

    private MyAccountBean myAccountBean;

    private static final UserProfile PROFILE = new UserProfile(
            5, "alice", "alice@example.com", true, false, true, null, true
    );

    @BeforeEach
    void setUp() {
        lenient().when(localeBean.getMsg(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        myAccountBean = new MyAccountBean(
                userSession,
                localeBean,
                userProfileService,
                userApiKeyService
        );
    }

    @Test
    void load_clearsStateWhenUserNotConnected() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        myAccountBean.load();

        assertNull(myAccountBean.getProfile());
        assertTrue(myAccountBean.getProjectRoles().isEmpty());
    }

    @Test
    void load_populatesProfileAndFormFields() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userProfileService.getProfileWithRoles(5)).thenReturn(
                new ProfileWithRoles(PROFILE, List.of())
        );

        myAccountBean.load();

        assertEquals(PROFILE, myAccountBean.getProfile());
        assertEquals("alice", myAccountBean.getEditableUsername());
        assertEquals("alice@example.com", myAccountBean.getEditableEmail());
        assertTrue(myAccountBean.isEditableAlertMail());
    }

    @Test
    void jsfHelpers_exposeProfileFlags() {
        myAccountBean.setProfile(PROFILE);

        assertFalse(myAccountBean.isSuperAdmin());
        assertTrue(myAccountBean.isKeyNeverExpire());
        assertNull(myAccountBean.getKeyExpiresAt());
        assertFalse(myAccountBean.isKeyExpired());
        assertTrue(myAccountBean.isApiKeySectionVisible());
    }

    @Test
    void jsfHelpers_detectExpiredKey() {
        myAccountBean.setProfile(new UserProfile(
                5, "alice", "alice@example.com", true, false, false, LocalDate.now().minusDays(1), true
        ));

        assertTrue(myAccountBean.isKeyExpired());
        assertFalse(myAccountBean.canRegenerateApiKey());
    }

    @Test
    void updateUsername_updatesProfileAndSession() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        myAccountBean.setEditableUsername("bob");
        when(userProfileService.updateUsername(5, "bob")).thenReturn(
                new UserProfile(5, "bob", "alice@example.com", true, false, true, null, true)
        );

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces()) {
            myAccountBean.updateUsername();
        }

        assertEquals("bob", myAccountBean.getProfile().username());
        verify(userSession).refreshDisplayName("bob");
    }

    @Test
    void updateEmail_keepsFormInSync() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        myAccountBean.setEditableEmail("new@example.com");
        when(userProfileService.updateEmail(5, "new@example.com")).thenReturn(
                new UserProfile(5, "alice", "new@example.com", true, false, true, null, true)
        );

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces()) {
            myAccountBean.updateEmail();
        }

        assertEquals("new@example.com", myAccountBean.getEditableEmail());
        verify(userSession).refreshEmail("new@example.com");
    }

    @Test
    void updateAlertMail_updatesSessionFlag() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        myAccountBean.setEditableAlertMail(false);
        when(userProfileService.updateAlertMail(5, false)).thenReturn(
                new UserProfile(5, "alice", "alice@example.com", false, false, true, null, true)
        );

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces()) {
            myAccountBean.updateAlertMail();
        }

        assertFalse(myAccountBean.isEditableAlertMail());
        verify(userSession).refreshAlertMail(false);
    }

    @Test
    void updateUsername_showsErrorOnInvalidData() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        myAccountBean.setEditableUsername("");
        when(userProfileService.updateUsername(5, ""))
                .thenThrow(new InvalidProfileDataException("Le pseudo est obligatoire."));

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            myAccountBean.updateUsername();
            messages.verify(() -> MessageUtils.showErrorMessage("Le pseudo est obligatoire."));
        }
    }

    @Test
    void regenerateApiKey_storesPlainKeyForDisplay() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userApiKeyService.regenerateApiKey(5)).thenReturn(
                new ApiKeyGenerationResult("plain-key", PROFILE)
        );

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            myAccountBean.regenerateApiKey();
        }

        assertEquals("plain-key", myAccountBean.getApiKeyPlain());
    }

    @Test
    void regenerateApiKey_showsErrorWhenRefused() {
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userApiKeyService.regenerateApiKey(5))
                .thenThrow(new ApiKeyRegenerationException("Clé expirée"));

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            myAccountBean.regenerateApiKey();
            messages.verify(() -> MessageUtils.showErrorMessage("Clé expirée"));
        }
    }

    @Test
    void load_delegatesRolesLoadingToProfileService() {
        UserProfile superAdmin = new UserProfile(5, "root", "root@example.com", false, true, true, null, true);
        when(userSession.getCurrentUserId()).thenReturn(5);
        when(userProfileService.getProfileWithRoles(5)).thenReturn(new ProfileWithRoles(superAdmin, List.of()));

        myAccountBean.load();

        verify(userProfileService).getProfileWithRoles(5);
        assertTrue(myAccountBean.getProjectRoles().isEmpty());
    }

    private static MockedStatic<PrimeFaces> mockPrimeFaces() {
        MockedStatic<PrimeFaces> primeFaces = mockStatic(PrimeFaces.class);
        PrimeFaces instance = mock(PrimeFaces.class);
        PrimeFaces.Ajax ajax = mock(PrimeFaces.Ajax.class);
        primeFaces.when(PrimeFaces::current).thenReturn(instance);
        when(instance.ajax()).thenReturn(ajax);
        return primeFaces;
    }
}
