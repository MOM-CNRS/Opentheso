package fr.cnrs.opentheso.v2.admin.model;

import java.io.Serializable;

/**
 * Affectation projet + rôle pour la création / édition d'un utilisateur (écran admin instance).
 * {@code 0} = non sélectionné (évite les bugs Mojarra Integer/null dans {@code ui:repeat}).
 */
public class NewUserProjectMembership implements Serializable {

    private static final long serialVersionUID = 1L;

    private int projectId;
    private int roleId;

    public NewUserProjectMembership() {
    }

    public NewUserProjectMembership(Integer projectId, Integer roleId) {
        this.projectId = projectId == null ? 0 : projectId;
        this.roleId = roleId == null ? 0 : roleId;
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public boolean isAssigned() {
        return projectId > 0 && roleId > 0;
    }

    public boolean isBlank() {
        return projectId <= 0 && roleId <= 0;
    }

    public boolean isIncomplete() {
        return !isBlank() && !isAssigned();
    }
}
