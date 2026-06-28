package fr.cnrs.opentheso.v2.shared.exception;

public class ModuleAccessDeniedException extends RuntimeException {

    public ModuleAccessDeniedException(String module) {
        super("Accès refusé au module " + module + ".");
    }
}
