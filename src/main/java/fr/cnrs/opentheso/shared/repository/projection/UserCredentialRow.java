package fr.cnrs.opentheso.shared.repository.projection;

public record UserCredentialRow(int userId, String username, String mail, String passwordHash) {
}
