package fr.cnrs.opentheso.v2.project.exception;

public class ProjectAccessDeniedException extends RuntimeException {

    public ProjectAccessDeniedException() {
        super("Accès refusé à ce projet.");
    }
}
