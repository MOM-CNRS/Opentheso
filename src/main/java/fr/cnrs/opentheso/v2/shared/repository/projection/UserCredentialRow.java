package fr.cnrs.opentheso.v2.shared.repository.projection;

public record UserCredentialRow(int userId, String username, String mail, String passwordHash) {
}
