package fr.cnrs.opentheso.shared.exception;

public class ModuleAccessDeniedException extends RuntimeException {

    public ModuleAccessDeniedException(String module) {
        super("Accès refusé au module " + module + ".");
    }
}
