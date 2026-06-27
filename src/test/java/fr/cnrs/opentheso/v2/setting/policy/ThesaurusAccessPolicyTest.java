package fr.cnrs.opentheso.v2.setting.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThesaurusAccessPolicyTest {

    @Test
    void isThesaurusAdmin_grantsSuperAdminRegardlessOfRole() {
        assertTrue(ThesaurusAccessPolicy.isThesaurusAdmin(true, null));
        assertTrue(ThesaurusAccessPolicy.isThesaurusAdmin(true, ThesaurusAccessPolicy.ROLE_MANAGER));
    }

    @Test
    void isThesaurusAdmin_grantsThesaurusSuperAdminAndAdminRoles() {
        assertTrue(ThesaurusAccessPolicy.isThesaurusAdmin(false, ThesaurusAccessPolicy.ROLE_SUPER_ADMIN));
        assertTrue(ThesaurusAccessPolicy.isThesaurusAdmin(false, ThesaurusAccessPolicy.ROLE_ADMIN));
    }

    @Test
    void isThesaurusAdmin_deniesManagerAndMissingRole() {
        assertFalse(ThesaurusAccessPolicy.isThesaurusAdmin(false, ThesaurusAccessPolicy.ROLE_MANAGER));
        assertFalse(ThesaurusAccessPolicy.isThesaurusAdmin(false, null));
    }
}
