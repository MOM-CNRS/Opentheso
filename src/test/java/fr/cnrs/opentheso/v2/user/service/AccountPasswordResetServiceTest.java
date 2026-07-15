package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.shared.mail.SystemMailSender;
import fr.cnrs.opentheso.v2.shared.repository.PasswordResetCommandRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserAuthQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.UserCredentialRow;
import fr.cnrs.opentheso.v2.shared.web.ApplicationUriService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPasswordResetServiceTest {

    @Mock
    private UserAuthQueryRepository userAuthQueryRepository;
    @Mock
    private PasswordResetCommandRepository passwordResetCommandRepository;
    @Mock
    private SystemMailSender systemMailSender;
    @Mock
    private ApplicationUriService applicationUriService;

    private AccountPasswordResetService accountPasswordResetService;

    @BeforeEach
    void setUp() {
        accountPasswordResetService = new AccountPasswordResetService(
                userAuthQueryRepository,
                passwordResetCommandRepository,
                systemMailSender,
                applicationUriService
        );
    }

    @Test
    void requestPasswordReset_doesNothingWhenUserMissing() {
        when(userAuthQueryRepository.findByMail("missing@example.com")).thenReturn(Optional.empty());

        accountPasswordResetService.requestPasswordReset("missing@example.com", false);

        verify(passwordResetCommandRepository, never()).invalidateActiveTokens(any(Integer.class));
        verify(systemMailSender, never()).sendHtmlMail(any(), any(), any());
    }

    @Test
    void requestPasswordReset_createsTokenAndSendsMail() {
        var credential = new UserCredentialRow(5, "alice", "alice@example.com", "hash");
        when(userAuthQueryRepository.findByMail("alice@example.com")).thenReturn(Optional.of(credential));
        when(applicationUriService.resolveApplicationBaseUrl()).thenReturn("https://opentheso.example");

        accountPasswordResetService.requestPasswordReset("alice@example.com", false);

        verify(passwordResetCommandRepository).invalidateActiveTokens(5);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetCommandRepository).insertToken(eq(5), tokenCaptor.capture(), any(LocalDateTime.class));
        assertTrue(tokenCaptor.getValue().length() >= 32);
        verify(systemMailSender).sendHtmlMail(
                eq("alice@example.com"),
                eq("Réinitialisation de votre mot de passe Opentheso"),
                contains("https://opentheso.example/reset-password.xhtml?token=" + tokenCaptor.getValue())
        );
    }
}
