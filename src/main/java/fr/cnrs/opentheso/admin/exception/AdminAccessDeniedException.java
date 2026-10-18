package fr.cnrs.opentheso.admin.exception;

public class AdminAccessDeniedException extends RuntimeException {

    public AdminAccessDeniedException() {
        super("Accès réservé aux super-administrateurs.");
    }
}
