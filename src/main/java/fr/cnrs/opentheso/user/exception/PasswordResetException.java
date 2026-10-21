package fr.cnrs.opentheso.user.exception;

public class PasswordResetException extends RuntimeException {

    public PasswordResetException(String message) {
        super(message);
    }
}
