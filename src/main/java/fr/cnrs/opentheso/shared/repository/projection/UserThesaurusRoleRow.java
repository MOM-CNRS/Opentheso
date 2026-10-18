package fr.cnrs.opentheso.shared.repository.projection;

public record UserThesaurusRoleRow(
        int projectId,
        String projectName,
        String thesaurusId,
        String thesaurusName,
        int roleId
) {
}
