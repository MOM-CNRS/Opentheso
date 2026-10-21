package fr.cnrs.opentheso.user.service;

import fr.cnrs.opentheso.entites.PasswordResetToken;
import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.repositories.PasswordResetTokenRepository;
import fr.cnrs.opentheso.shared.mail.SystemMailSender;
import fr.cnrs.opentheso.shared.repository.PasswordResetCommandRepository;
import fr.cnrs.opentheso.shared.repository.UserAuthQueryRepository;
import fr.cnrs.opentheso.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.shared.repository.projection.UserCredentialRow;
import fr.cnrs.opentheso.shared.web.ApplicationUriService;
import fr.cnrs.opentheso.user.exception.PasswordResetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private UserPasswordService userPasswordService;
    @Mock
    private UserCommandRepository userCommandRepository;
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
                passwordResetTokenRepository,
                userPasswordService,
                userCommandRepository,
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
        when(systemMailSender.sendHtmlMail(any(), any(), any())).thenReturn(true);

        assertTrue(accountPasswordResetService.requestPasswordReset("alice@example.com", false));

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

    @Test
    void resetPassword_updatesPasswordAndConsumesToken() {
        when(passwordResetTokenRepository.findByToken("abc")).thenReturn(Optional.of(usableToken()));

        accountPasswordResetService.resetPassword("abc", "Secret1!", "Secret1!");

        verify(userPasswordService).changePassword(5, "Secret1!", "Secret1!");
        verify(userCommandRepository).activatePendingPassword(5);
        verify(passwordResetCommandRepository).markUsed("abc");
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        PasswordResetToken token = usableToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(passwordResetTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertThrows(PasswordResetException.class,
                () -> accountPasswordResetService.resetPassword("abc", "Secret1!", "Secret1!"));
        verify(userPasswordService, never()).changePassword(anyInt(), any(), any());
    }

    private static PasswordResetToken usableToken() {
        User user = new User();
        user.setId(5);
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("abc");
        token.setUser(user);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        return token;
    }
}
