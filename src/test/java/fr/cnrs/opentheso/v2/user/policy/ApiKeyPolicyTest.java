package fr.cnrs.opentheso.v2.user.policy;

import fr.cnrs.opentheso.v2.user.model.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyPolicyTest {

    @Test
    void isSectionVisible_handlesNullProfile() {
        assertFalse(ApiKeyPolicy.isSectionVisible(null));
    }

    @Test
    void isSectionVisible_whenExpirationDateConfigured() {
        UserProfile withExpiry = new UserProfile(1, "a", "a@b.c", false, false, false, LocalDate.of(2099, Month.DECEMBER, 31), true);
        assertTrue(ApiKeyPolicy.isSectionVisible(withExpiry));
    }

    @Test
    void isExpired_returnsFalseForNullProfile() {
        assertFalse(ApiKeyPolicy.isExpired(null));
    }

    @Test
    void isExpired_detectsPastExpirationDate() {
        UserProfile expired = new UserProfile(1, "a", "a@b.c", false, false, false, LocalDate.of(2000, Month.JANUARY, 1), true);
        assertTrue(ApiKeyPolicy.isExpired(expired));
    }

    @Test
    void isExpired_ignoresNeverExpireKeys() {
        UserProfile neverExpire = new UserProfile(1, "a", "a@b.c", false, false, true, LocalDate.of(2000, Month.JANUARY, 1), true);
        assertFalse(ApiKeyPolicy.isExpired(neverExpire));
    }

    @Test
    void canRegenerate_requiresVisibleNonExpiredKey() {
        UserProfile active = new UserProfile(1, "a", "a@b.c", false, false, true, null, true);
        UserProfile expired = new UserProfile(1, "a", "a@b.c", false, false, false, LocalDate.of(2000, Month.JANUARY, 1), true);
        UserProfile none = new UserProfile(1, "a", "a@b.c", false, false, false, null, false);

        assertTrue(ApiKeyPolicy.canRegenerate(active));
        assertFalse(ApiKeyPolicy.canRegenerate(expired));
        assertFalse(ApiKeyPolicy.canRegenerate(none));
    }
}
