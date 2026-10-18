package fr.cnrs.opentheso.publicapi.exception;

public class PublicResourceNotFoundException extends RuntimeException {

    public PublicResourceNotFoundException(String message) {
        super(message);
    }
}
