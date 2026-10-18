package fr.cnrs.opentheso.user.model;

import java.io.Serializable;

public record ThesaurusRoleOverview(
        String thesaurusId,
        String thesaurusName,
        String roleName
) implements Serializable {

    public String getThesaurusId() {
        return thesaurusId;
    }

    public String getThesaurusName() {
        return thesaurusName;
    }

    public String getRoleName() {
        return roleName;
    }
}
