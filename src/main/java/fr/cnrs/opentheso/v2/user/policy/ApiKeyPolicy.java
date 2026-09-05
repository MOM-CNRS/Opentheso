package fr.cnrs.opentheso.v2.user.policy;

import fr.cnrs.opentheso.v2.shared.time.V2Dates;
import fr.cnrs.opentheso.v2.user.model.UserProfile;

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
        return V2Dates.nowDate().isAfter(profile.keyExpiresAt());
    }

    public static boolean canRegenerate(UserProfile profile) {
        return isSectionVisible(profile) && !isExpired(profile);
    }
}
