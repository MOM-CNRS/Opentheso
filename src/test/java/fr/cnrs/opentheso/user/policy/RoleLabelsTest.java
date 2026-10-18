package fr.cnrs.opentheso.user.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleLabelsTest {

    @Test
    void fromRoleId_mapsAllKnownRoles() {
        assertEquals("superAdmin", RoleLabels.fromRoleId(1));
        assertEquals("admin", RoleLabels.fromRoleId(2));
        assertEquals("manager", RoleLabels.fromRoleId(3));
        assertEquals("contributor", RoleLabels.fromRoleId(4));
        assertEquals("unknown", RoleLabels.fromRoleId(99));
    }
}
