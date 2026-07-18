package fr.cnrs.opentheso.v2.publicapi.exception;

public class PublicResourceNotFoundException extends RuntimeException {

    public PublicResourceNotFoundException(String message) {
        super(message);
    }
}
