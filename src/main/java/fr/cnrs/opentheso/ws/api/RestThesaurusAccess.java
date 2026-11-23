package fr.cnrs.opentheso.ws.api;

/**
 * Issue d'une consultation de thésaurus par les API REST.
 */
public enum RestThesaurusAccess {

    ALLOWED(200, null),
    NOT_FOUND(404, "Thésaurus introuvable."),
    PRIVATE(403, "Vous n'avez pas accès à ce thésaurus : il est privé."),
    FOREIGN_PROJECT(403, "Vous n'avez pas accès à ce thésaurus : il appartient à un projet auquel vous n'avez pas droit.");

    private final int status;
    private final String message;

    RestThesaurusAccess(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() {
        return status;
    }

    public String body() {
        if (message == null) {
            return "";
        }
        return "{\"message\":\"" + message + "\"}";
    }

    /**
     * Un thésaurus public reste visible. Un thésaurus privé n'est visible que pour un membre
     * de son projet. Sinon, le refus distingue l'absence de projet du rattachement à un projet interdit.
     */
    public static RestThesaurusAccess decide(boolean exists, boolean isPrivate, boolean attachedToProject, boolean memberOfProject) {
        if (!exists) {
            return NOT_FOUND;
        }
        if (!isPrivate || memberOfProject) {
            return ALLOWED;
        }
        if (attachedToProject) {
            return FOREIGN_PROJECT;
        }
        return PRIVATE;
    }
}
