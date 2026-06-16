package fr.cnrs.opentheso.v2.user.policy;

import fr.cnrs.opentheso.v2.user.model.UserProfile;

import java.time.LocalDate;

public final class ApiKeyPolicy {

    private ApiKeyPolicy() {
    }

    public static boolean isSectionVisible(UserProfile profile) {
        if (profile == null) {
            return false;
        }
        return profile.keyNeverExpire() || profile.keyExpiresAt() != null;
    }

    public static boolean isExpired(UserProfile profile) {
        if (profile == null || profile.keyNeverExpire() || profile.keyExpiresAt() == null) {
            return false;
        }
        return LocalDate.now().isAfter(profile.keyExpiresAt());
    }

    public static boolean canRegenerate(UserProfile profile) {
        return isSectionVisible(profile) && !isExpired(profile);
    }
}
