package fr.cnrs.opentheso.v2.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(int userId) {
        super("Utilisateur introuvable : id=" + userId);
    }
}
