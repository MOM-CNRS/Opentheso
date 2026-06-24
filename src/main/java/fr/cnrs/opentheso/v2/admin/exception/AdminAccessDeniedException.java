package fr.cnrs.opentheso.v2.admin.exception;

public class AdminAccessDeniedException extends RuntimeException {

    public AdminAccessDeniedException() {
        super("Accès réservé aux super-administrateurs.");
    }
}
