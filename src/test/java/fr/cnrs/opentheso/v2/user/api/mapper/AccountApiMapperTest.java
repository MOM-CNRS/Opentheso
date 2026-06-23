package fr.cnrs.opentheso.v2.user.api.mapper;

import fr.cnrs.opentheso.v2.user.api.dto.AccountProfileResponse;
import fr.cnrs.opentheso.v2.user.api.dto.AccountRolesResponse;
import fr.cnrs.opentheso.v2.user.api.dto.ApiKeyRegenerateResponse;
import fr.cnrs.opentheso.v2.user.model.ApiKeyGenerationResult;
import fr.cnrs.opentheso.v2.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.v2.user.model.ThesaurusRoleOverview;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountApiMapperTest {

    @Test
    void toProfileResponse_includesApiKeyPolicyFlags() {
        UserProfile profile = new UserProfile(
                1, "alice", "alice@example.com", true, false,
                true, null, true
        );

        AccountProfileResponse response = AccountApiMapper.toProfileResponse(profile);

        assertEquals(1, response.id());
        assertEquals("alice", response.username());
        assertTrue(response.apiKeySectionVisible());
        assertFalse(response.apiKeyExpired());
        assertTrue(response.canRegenerateApiKey());
    }

    @Test
    void toProfileResponse_marksExpiredKey() {
        UserProfile profile = new UserProfile(
                1, "alice", "alice@example.com", false, false,
                false, LocalDate.now().minusDays(1), true
        );

        AccountProfileResponse response = AccountApiMapper.toProfileResponse(profile);

        assertTrue(response.apiKeySectionVisible());
        assertTrue(response.apiKeyExpired());
        assertFalse(response.canRegenerateApiKey());
    }

    @Test
    void toRolesResponse_mapsNestedStructure() {
        UserProfile profile = new UserProfile(1, "a", "a@b.c", false, false, true, null, true);
        List<ProjectRoleOverview> roles = List.of(
                new ProjectRoleOverview(3, "Projet A", List.of(
                        new ThesaurusRoleOverview("TH1", "Thésaurus 1", "manager")
                ))
        );

        AccountRolesResponse response = AccountApiMapper.toRolesResponse(profile, roles);

        assertFalse(response.superAdmin());
        assertEquals(1, response.projectRoles().size());
        assertEquals("Projet A", response.projectRoles().get(0).projectName());
        assertEquals("manager", response.projectRoles().get(0).thesaurusRoles().get(0).roleName());
    }

    @Test
    void toRegenerateResponse_returnsPlainKeyAndUpdatedProfile() {
        UserProfile saved = new UserProfile(1, "a", "a@b.c", false, false, true, null, true);
        ApiKeyGenerationResult result = new ApiKeyGenerationResult("plain-key-value", saved);

        ApiKeyRegenerateResponse response = AccountApiMapper.toRegenerateResponse(result);

        assertEquals("plain-key-value", response.plainTextApiKey());
        assertTrue(response.profile().canRegenerateApiKey());
    }
}
