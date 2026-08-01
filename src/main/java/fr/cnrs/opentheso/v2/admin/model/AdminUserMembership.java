package fr.cnrs.opentheso.v2.admin.model;

import java.io.Serializable;

public record AdminUserMembership(
        int userId,
        String username,
        int projectId,
        String projectName,
        int roleId,
        String roleName
) implements Serializable {

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public int getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }
}
