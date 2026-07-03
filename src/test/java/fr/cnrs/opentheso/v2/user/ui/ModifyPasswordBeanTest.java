package fr.cnrs.opentheso.v2.user.ui;

import fr.cnrs.opentheso.v2.shared.ui.V2LocaleBean;
import fr.cnrs.opentheso.utils.MessageUtils;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.user.service.UserPasswordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.PrimeFaces;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModifyPasswordBeanTest {

    @Mock
    private UserSession userSession;
    @Mock
    private V2LocaleBean localeBean;
    @Mock
    private UserPasswordService userPasswordService;

    private ModifyPasswordBean modifyPasswordBean;

    @BeforeEach
    void setUp() {
        lenient().when(localeBean.getMsg("profile.userNotConnected")).thenReturn("Utilisateur non connecté.");
        lenient().when(localeBean.getMsg("profile.passwordChangedSuccess")).thenReturn("Mot de passe changé avec succès");
        lenient().when(localeBean.getMsg("profile.unexpectedError")).thenReturn("Une erreur inattendue est survenue.");
        modifyPasswordBean = new ModifyPasswordBean(userSession, localeBean, userPasswordService);
    }

    @Test
    void prepareDialog_clearsFields() {
        modifyPasswordBean.setPassword("secret");
        modifyPasswordBean.setConfirmation("secret");

        modifyPasswordBean.prepareDialog();

        assertNull(modifyPasswordBean.getPassword());
        assertNull(modifyPasswordBean.getConfirmation());
    }

    @Test
    void apply_changesPasswordAndClearsDialog() {
        when(userSession.getCurrentUserId()).thenReturn(8);
        modifyPasswordBean.setPassword("Abcd1234!");
        modifyPasswordBean.setConfirmation("Abcd1234!");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class);
             MockedStatic<PrimeFaces> primeFaces = mockPrimeFaces()) {
            modifyPasswordBean.apply();
        }

        verify(userPasswordService).changePassword(8, "Abcd1234!", "Abcd1234!");
        assertNull(modifyPasswordBean.getPassword());
        assertNull(modifyPasswordBean.getConfirmation());
    }

    @Test
    void apply_showsErrorWhenUserNotConnected() {
        when(userSession.getCurrentUserId()).thenReturn(null);

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            modifyPasswordBean.apply();
            messages.verify(() -> MessageUtils.showErrorMessage("Utilisateur non connecté."));
        }
    }

    @Test
    void apply_showsValidationError() {
        when(userSession.getCurrentUserId()).thenReturn(8);
        modifyPasswordBean.setPassword("weak");
        modifyPasswordBean.setConfirmation("weak");
        doThrow(new InvalidPasswordException("Mot de passe non identique."))
                .when(userPasswordService).changePassword(8, "weak", "weak");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            modifyPasswordBean.apply();
            messages.verify(() -> MessageUtils.showErrorMessage("Mot de passe non identique."));
        }
    }

    @Test
    void apply_showsGenericErrorOnUnexpectedFailure() {
        when(userSession.getCurrentUserId()).thenReturn(8);
        modifyPasswordBean.setPassword("Abcd1234!");
        modifyPasswordBean.setConfirmation("Abcd1234!");
        doThrow(new UserNotFoundException(8))
                .when(userPasswordService).changePassword(8, "Abcd1234!", "Abcd1234!");

        try (MockedStatic<MessageUtils> messages = mockStatic(MessageUtils.class)) {
            modifyPasswordBean.apply();
            messages.verify(() -> MessageUtils.showErrorMessage("Une erreur inattendue est survenue."));
        }
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
