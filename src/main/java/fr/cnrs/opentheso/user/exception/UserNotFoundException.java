package fr.cnrs.opentheso.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(int userId) {
        super("Utilisateur introuvable : id=" + userId);
    }
}
