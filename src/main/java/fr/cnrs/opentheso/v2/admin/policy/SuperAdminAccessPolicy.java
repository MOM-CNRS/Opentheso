package fr.cnrs.opentheso.v2.admin.policy;

import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;
import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Garde super-admin : délègue au {@link RightsService} quand un userId est disponible.
 * Surcharge booléenne conservée pour les services qui reçoivent déjà le flag résolu.
 */
@Component
@RequiredArgsConstructor
public class SuperAdminAccessPolicy {

    private final RightsService rightsService;

    public void requireSuperAdmin(Integer userId) {
        if (!rightsService.can(userId, Permission.SUPER_ADMIN, AuthTarget.none())) {
            throw new AdminAccessDeniedException();
        }
    }

    /** @deprecated préférer {@link #requireSuperAdmin(Integer)} */
    @Deprecated
    public static void requireSuperAdmin(boolean superAdmin) {
        if (!superAdmin) {
            throw new AdminAccessDeniedException();
        }
    }
}
