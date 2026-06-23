package fr.cnrs.opentheso.v2.user.mapper;

import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserProfileMapperTest {

    @Test
    void toProfile_mapsAllFields() {
        UserEntity entity = new UserEntity();
        entity.setId(1);
        entity.setUsername("alice");
        entity.setMail("alice@example.com");
        entity.setAlertMail(true);
        entity.setSuperAdmin(true);
        entity.setKeyNeverExpire(false);
        entity.setKeyExpiresAt(LocalDate.of(2026, 12, 31));
        entity.setApiKey("encrypted");

        UserProfile profile = UserProfileMapper.toProfile(entity);

        assertEquals(1, profile.id());
        assertEquals("alice", profile.username());
        assertEquals("alice@example.com", profile.email());
        assertTrue(profile.alertMail());
        assertTrue(profile.superAdmin());
        assertFalse(profile.keyNeverExpire());
        assertEquals(LocalDate.of(2026, 12, 31), profile.keyExpiresAt());
        assertTrue(profile.hasApiKey());
    }

    @Test
    void toProfile_treatsNullBooleansAsFalse() {
        UserEntity entity = new UserEntity();
        entity.setId(2);
        entity.setUsername("bob");
        entity.setMail("bob@example.com");
        entity.setAlertMail(null);
        entity.setSuperAdmin(null);
        entity.setKeyNeverExpire(null);
        entity.setApiKey("  ");

        UserProfile profile = UserProfileMapper.toProfile(entity);

        assertFalse(profile.alertMail());
        assertFalse(profile.superAdmin());
        assertFalse(profile.keyNeverExpire());
        assertFalse(profile.hasApiKey());
        assertNull(profile.keyExpiresAt());
    }
}
