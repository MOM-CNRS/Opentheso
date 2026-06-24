package fr.cnrs.opentheso.v2.project.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectAccessPolicyTest {

    @Test
    void isProjectAdmin_grantsSuperAdminAndAdmin() {
        assertTrue(ProjectAccessPolicy.isProjectAdmin(true, null));
        assertTrue(ProjectAccessPolicy.isProjectAdmin(false, 1));
        assertTrue(ProjectAccessPolicy.isProjectAdmin(false, 2));
        assertFalse(ProjectAccessPolicy.isProjectAdmin(false, 3));
        assertFalse(ProjectAccessPolicy.isProjectAdmin(false, null));
    }

    @Test
    void minAssignableRoleId_followsHierarchy() {
        assertEquals(1, ProjectAccessPolicy.minAssignableRoleId(true, null));
        assertEquals(2, ProjectAccessPolicy.minAssignableRoleId(false, 2));
        assertEquals(3, ProjectAccessPolicy.minAssignableRoleId(false, 3));
        assertEquals(4, ProjectAccessPolicy.minAssignableRoleId(false, 4));
        assertEquals(4, ProjectAccessPolicy.minAssignableRoleId(false, null));
    }

    @Test
    void minVisibleMemberRoleId_followsHierarchy() {
        assertEquals(1, ProjectAccessPolicy.minVisibleMemberRoleId(true, null));
        assertEquals(2, ProjectAccessPolicy.minVisibleMemberRoleId(false, 2));
        assertEquals(4, ProjectAccessPolicy.minVisibleMemberRoleId(false, null));
    }

    @Test
    void isProjectAdmin_rejectsManagerAndContributor() {
        assertFalse(ProjectAccessPolicy.isProjectAdmin(false, 3));
        assertFalse(ProjectAccessPolicy.isProjectAdmin(false, 4));
    }
}
