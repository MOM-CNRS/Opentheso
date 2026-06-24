package fr.cnrs.opentheso.v2.project.policy;

public final class ProjectAccessPolicy {

    public static final int ROLE_SUPER_ADMIN = 1;
    public static final int ROLE_ADMIN = 2;
    public static final int ROLE_MANAGER = 3;
    public static final int ROLE_CONTRIBUTOR = 4;

    private ProjectAccessPolicy() {
    }

    public static boolean isProjectAdmin(boolean superAdmin, Integer callerRoleId) {
        if (superAdmin) {
            return true;
        }
        return callerRoleId != null && callerRoleId < ROLE_MANAGER;
    }

    public static int minVisibleMemberRoleId(boolean superAdmin, Integer callerRoleId) {
        if (superAdmin) {
            return ROLE_SUPER_ADMIN;
        }
        return callerRoleId != null ? callerRoleId : ROLE_CONTRIBUTOR;
    }

    public static int minAssignableRoleId(boolean superAdmin, Integer callerRoleId) {
        if (superAdmin) {
            return ROLE_SUPER_ADMIN;
        }
        if (callerRoleId == null) {
            return ROLE_CONTRIBUTOR;
        }
        if (callerRoleId == ROLE_ADMIN) {
            return ROLE_ADMIN;
        }
        if (callerRoleId == ROLE_MANAGER) {
            return ROLE_MANAGER;
        }
        return ROLE_CONTRIBUTOR;
    }
}
