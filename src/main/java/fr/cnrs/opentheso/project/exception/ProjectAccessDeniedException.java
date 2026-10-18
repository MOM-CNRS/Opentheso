package fr.cnrs.opentheso.project.exception;

public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException() {
        super("Accès refusé à ce projet.");
    }
}
