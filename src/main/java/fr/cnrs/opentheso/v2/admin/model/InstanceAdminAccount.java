package fr.cnrs.opentheso.v2.admin.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record InstanceAdminAccount(
        int userId,
        String username,
        String email,
        String organization,
        LocalDateTime lastLogin,
        String appRoleKey,
        String appRoleLabel
) implements Serializable {

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getOrganization() {
        return organization;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public String getAppRoleKey() {
        return appRoleKey;
    }

    public String getAppRoleLabel() {
        return appRoleLabel;
    }
}
