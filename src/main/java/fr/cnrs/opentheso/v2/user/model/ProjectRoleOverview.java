package fr.cnrs.opentheso.v2.user.model;

import java.io.Serializable;
import java.util.List;

public record ProjectRoleOverview(
        int projectId,
        String projectName,
        List<ThesaurusRoleOverview> thesaurusRoles
) implements Serializable {

    public int getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public List<ThesaurusRoleOverview> getThesaurusRoles() {
        return thesaurusRoles;
    }
}
