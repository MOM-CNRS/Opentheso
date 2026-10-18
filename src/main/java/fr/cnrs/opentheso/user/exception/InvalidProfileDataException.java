package fr.cnrs.opentheso.user.exception;

public class InvalidProfileDataException extends RuntimeException {

    public InvalidProfileDataException(String message) {
        super(message);
    }
}
