package fr.cnrs.opentheso.admin.policy;

import fr.cnrs.opentheso.admin.exception.AdminAccessDeniedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SuperAdminAccessPolicyTest {

    @Test
    void requireSuperAdmin_allowsSuperAdmin() {
        assertDoesNotThrow(() -> SuperAdminAccessPolicy.requireResolvedSuperAdmin(true));
    }

    @Test
    void requireSuperAdmin_rejectsNonSuperAdmin() {
        assertThrows(AdminAccessDeniedException.class, () -> SuperAdminAccessPolicy.requireResolvedSuperAdmin(false));
    }
}
