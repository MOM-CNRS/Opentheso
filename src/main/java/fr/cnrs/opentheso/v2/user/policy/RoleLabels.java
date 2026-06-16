package fr.cnrs.opentheso.v2.user.policy;

public final class RoleLabels {

    private RoleLabels() {
    }

    public static String fromRoleId(int roleId) {
        return switch (roleId) {
            case 1 -> "superAdmin";
            case 2 -> "admin";
            case 3 -> "manager";
            case 4 -> "contributor";
            default -> "unknown";
        };
    }
}
