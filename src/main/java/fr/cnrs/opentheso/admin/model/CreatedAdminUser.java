package fr.cnrs.opentheso.admin.model;

import java.io.Serializable;

public record CreatedAdminUser(int userId, String username, String email) implements Serializable {
}
