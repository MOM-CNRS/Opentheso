package fr.cnrs.opentheso.v2.shared.session;

import java.io.Serializable;

public record SessionUser(
        int userId,
        String username,
        String email,
        boolean superAdmin,
        boolean projectAdmin,
        boolean contributor,
        boolean manager
) implements Serializable {
}
