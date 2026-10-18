package fr.cnrs.opentheso.user.mapper;

import fr.cnrs.opentheso.user.model.UserProfile;
import fr.cnrs.opentheso.shared.persistence.UserEntity;
import org.apache.commons.lang3.StringUtils;

public final class UserProfileMapper {

    private UserProfileMapper() {
    }

    public static UserProfile toProfile(UserEntity entity) {
        return new UserProfile(
                entity.getId(),
                entity.getUsername(),
                entity.getMail(),
                Boolean.TRUE.equals(entity.getAlertMail()),
                Boolean.TRUE.equals(entity.getSuperAdmin()),
                Boolean.TRUE.equals(entity.getKeyNeverExpire()),
                entity.getKeyExpiresAt(),
                StringUtils.isNotBlank(entity.getApiKey())
        );
    }
}
