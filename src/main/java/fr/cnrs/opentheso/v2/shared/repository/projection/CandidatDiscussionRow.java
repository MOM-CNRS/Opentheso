package fr.cnrs.opentheso.v2.shared.repository.projection;

public record CandidatDiscussionRow(
        int idUser,
        String username,
        String value,
        String date
) {
}
