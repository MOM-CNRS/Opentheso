package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLookupServiceTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserLookupService userLookupService;

    @BeforeEach
    void setUp() {
        userLookupService = new UserLookupService(userProfileRepository);
    }

    @Test
    void requireEntity_returnsUserWhenPresent() {
        UserEntity entity = new UserEntity();
        entity.setId(3);
        entity.setUsername("alice");
        entity.setMail("alice@example.com");
        entity.setAlertMail(true);
        entity.setSuperAdmin(false);
        entity.setKeyNeverExpire(true);

        when(userProfileRepository.findById(3)).thenReturn(Optional.of(entity));

        UserEntity found = userLookupService.requireEntity(3);

        assertEquals(3, found.getId());
        assertEquals("alice", found.getUsername());
    }

    @Test
    void requireEntity_throwsWhenMissing() {
        when(userProfileRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userLookupService.requireEntity(404));
    }
}
