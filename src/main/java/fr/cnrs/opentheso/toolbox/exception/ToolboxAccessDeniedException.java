package fr.cnrs.opentheso.toolbox.exception;

public class ToolboxAccessDeniedException extends RuntimeException {

    public ToolboxAccessDeniedException() {
        super("Accès refusé à la boîte à outils.");
    }
}
