package fr.cnrs.opentheso.shared.repository.projection;

public record CandidatDiscussionRow(
        int idUser,
        String username,
        String value,
        String date
) {
}
