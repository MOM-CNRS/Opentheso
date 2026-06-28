package fr.cnrs.opentheso.v2.toolbox.exception;

public class ToolboxAccessDeniedException extends RuntimeException {

    public ToolboxAccessDeniedException() {
        super("Accès refusé à la boîte à outils.");
    }
}
