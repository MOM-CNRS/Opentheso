package fr.cnrs.opentheso.v2.shared.ui;

import fr.cnrs.opentheso.v2.user.service.AccountPasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordBeanTest {

    @Mock
    private AccountPasswordResetService accountPasswordResetService;

    private ForgotPasswordBean forgotPasswordBean;

    @BeforeEach
    void setUp() {
        forgotPasswordBean = new ForgotPasswordBean(accountPasswordResetService);
    }

    @Test
    void sendMail_requiresEmail() {
        forgotPasswordBean.sendMail();

        assertEquals("Veuillez saisir une adresse mail", forgotPasswordBean.getError());
        assertNull(forgotPasswordBean.getMessage());
        verify(accountPasswordResetService, never()).requestPasswordReset(any(), anyBoolean());
    }

    @Test
    void sendMail_requestsResetWithoutRevealingAccount() {
        forgotPasswordBean.setSendTo(" alice@example.com ");

        forgotPasswordBean.sendMail();

        verify(accountPasswordResetService).requestPasswordReset("alice@example.com", false);
        assertNull(forgotPasswordBean.getError());
        assertEquals(
                "Si un compte existe pour cette adresse, un email de réinitialisation a été envoyé.",
                forgotPasswordBean.getMessage()
        );
    }
}
