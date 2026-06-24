package fr.cnrs.opentheso.v2.admin.policy;

import fr.cnrs.opentheso.v2.admin.exception.AdminAccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuperAdminAccessPolicyTest {

    @Test
    void requireSuperAdmin_allowsSuperAdmin() {
        assertDoesNotThrow(() -> SuperAdminAccessPolicy.requireSuperAdmin(true));
    }

    @Test
    void requireSuperAdmin_rejectsNonSuperAdmin() {
        assertThrows(AdminAccessDeniedException.class, () -> SuperAdminAccessPolicy.requireSuperAdmin(false));
    }
}
