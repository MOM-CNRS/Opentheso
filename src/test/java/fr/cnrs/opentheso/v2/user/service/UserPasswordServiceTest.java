package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.user.exception.InvalidPasswordException;
import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPasswordServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserLookupService userLookupService;
    private UserProfileService userProfileService;
    private UserPasswordService userPasswordService;

    @BeforeEach
    void setUp() {
        userLookupService = new UserLookupService(userProfileRepository);
        userProfileService = new UserProfileService(
                userProfileRepository,
                userLookupService,
                org.mockito.Mockito.mock(UserRoleOverviewService.class)
        );
        userPasswordService = new UserPasswordService(userLookupService, userProfileService, passwordEncoder);
    }

    @Test
    void changePassword_encodesWithBcrypt() {
        UserEntity user = new UserEntity();
        user.setId(5);
        user.setUsername("alice");
        user.setMail("alice@example.com");

        when(userProfileRepository.findById(5)).thenReturn(Optional.of(user));
        when(userProfileRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userPasswordService.changePassword(5, "Abcd1234!", "Abcd1234!");

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userProfileRepository).save(captor.capture());
        assertTrue(passwordEncoder.matches("Abcd1234!", captor.getValue().getPassword()));
    }

    @Test
    void changePassword_rejectsInvalidInput() {
        assertThrows(InvalidPasswordException.class,
                () -> userPasswordService.changePassword(5, "weak", "weak"));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void changePassword_throwsWhenUserMissing() {
        when(userProfileRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userPasswordService.changePassword(99, "Abcd1234!", "Abcd1234!"));
    }
}
