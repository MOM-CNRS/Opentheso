package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.v2.shared.repository.UserAuthQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.shared.repository.projection.UserCredentialRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserAuthQueryRepository userAuthQueryRepository;
    @Mock
    private UserCommandRepository userCommandRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userAuthQueryRepository,
                userCommandRepository,
                passwordEncoder
        );
    }

    @Test
    void authenticate_returnsEmptyWhenCredentialsBlank() {
        assertTrue(authenticationService.authenticate("", "pwd").isEmpty());
        assertTrue(authenticationService.authenticate("alice", "").isEmpty());
    }

    @Test
    void authenticate_matchesBcryptPassword() {
        var row = new UserCredentialRow(7, "alice", "alice@example.com", "$2a$10$hash");
        when(userAuthQueryRepository.findByUsername("alice")).thenReturn(Optional.of(row));
        when(passwordEncoder.matches("secret", row.passwordHash())).thenReturn(true);

        var authenticated = authenticationService.authenticate("alice", "secret");

        assertEquals(new AuthenticatedUser(7, "alice"), authenticated.orElseThrow());
        verify(userCommandRepository, never()).updatePassword(eq(7), anyString());
    }

    @Test
    void authenticate_fallsBackToMailAndMigratesMd5Password() {
        String md5Hash = MD5Password.getEncodedPassword("secret");
        var row = new UserCredentialRow(8, "bob", "bob@example.com", md5Hash);
        when(userAuthQueryRepository.findByUsername("bob@example.com")).thenReturn(Optional.empty());
        when(userAuthQueryRepository.findByMail("bob@example.com")).thenReturn(Optional.of(row));
        when(passwordEncoder.encode("secret")).thenReturn("$2a$10$new");

        var authenticated = authenticationService.authenticate("bob@example.com", "secret");

        assertEquals(new AuthenticatedUser(8, "bob"), authenticated.orElseThrow());
        verify(userCommandRepository).updatePassword(8, "$2a$10$new");
    }
}
