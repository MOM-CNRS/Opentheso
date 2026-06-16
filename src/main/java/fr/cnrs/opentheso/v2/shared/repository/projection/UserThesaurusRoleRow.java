package fr.cnrs.opentheso.v2.shared.repository.projection;

public record UserThesaurusRoleRow(
        int projectId,
        String projectName,
        String thesaurusId,
        String thesaurusName,
        int roleId
) {
}
