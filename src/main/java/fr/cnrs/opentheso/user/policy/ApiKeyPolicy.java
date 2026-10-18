package fr.cnrs.opentheso.user.policy;

import fr.cnrs.opentheso.shared.time.Dates;
import fr.cnrs.opentheso.user.model.UserProfile;

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
        return Dates.nowDate().isAfter(profile.keyExpiresAt());
    }

    public static boolean canRegenerate(UserProfile profile) {
        return isSectionVisible(profile) && !isExpired(profile);
    }
}
