package fr.cnrs.opentheso.v2.project.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(int projectId) {
        super("Projet introuvable : id=" + projectId);
    }
}
