package fr.cnrs.opentheso.v2.project.model;

import java.io.Serializable;

public record CreatedProjectMember(int userId, String username, String email) implements Serializable {
}
