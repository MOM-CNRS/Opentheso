package fr.cnrs.opentheso.v2.admin.model;

import java.io.Serializable;

public record CreatedAdminUser(int userId, String username, String email) implements Serializable {
}
