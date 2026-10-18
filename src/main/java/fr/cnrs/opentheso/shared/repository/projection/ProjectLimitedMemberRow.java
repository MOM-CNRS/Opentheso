package fr.cnrs.opentheso.shared.repository.projection;

public record ProjectLimitedMemberRow(
        int userId,
        String username,
        boolean active,
        int roleId,
        String roleName,
        String thesaurusId,
        String thesaurusName
) {
}
