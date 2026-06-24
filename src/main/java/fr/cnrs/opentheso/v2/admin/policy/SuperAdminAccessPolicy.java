package fr.cnrs.opentheso.v2.admin.policy;

import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;

public final class SuperAdminAccessPolicy {

    private SuperAdminAccessPolicy() {
    }

    public static void requireSuperAdmin(boolean superAdmin) {
        if (!superAdmin) {
            throw new AdminAccessDeniedException();
        }
    }
}
