package fr.cnrs.opentheso.v2.setting.policy;

public final class ThesaurusAccessPolicy {

    public static final int ROLE_SUPER_ADMIN = 1;
    public static final int ROLE_ADMIN = 2;
    public static final int ROLE_MANAGER = 3;

    private ThesaurusAccessPolicy() {
    }

    public static boolean isThesaurusAdmin(boolean superAdmin, Integer roleId) {
        if (superAdmin) {
            return true;
        }
        return roleId != null && roleId < ROLE_MANAGER;
    }
}
