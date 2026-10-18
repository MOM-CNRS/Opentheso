package fr.cnrs.opentheso.project.model;

import java.io.Serializable;

public record CreatedProjectMember(int userId, String username, String email) implements Serializable {
}
