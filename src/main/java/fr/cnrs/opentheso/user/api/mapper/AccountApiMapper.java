package fr.cnrs.opentheso.user.api.mapper;

import fr.cnrs.opentheso.user.api.dto.AccountProfileResponse;
import fr.cnrs.opentheso.user.api.dto.AccountRolesResponse;
import fr.cnrs.opentheso.user.api.dto.ApiKeyRegenerateResponse;
import fr.cnrs.opentheso.user.api.dto.ProjectRoleApiDto;
import fr.cnrs.opentheso.user.api.dto.ThesaurusRoleApiDto;
import fr.cnrs.opentheso.user.policy.ApiKeyPolicy;
import fr.cnrs.opentheso.user.model.ApiKeyGenerationResult;
import fr.cnrs.opentheso.user.model.ProjectRoleOverview;
import fr.cnrs.opentheso.user.model.UserProfile;

import java.util.List;

public final class AccountApiMapper {

    private AccountApiMapper() {
    }

    public static AccountProfileResponse toProfileResponse(UserProfile profile) {
        return new AccountProfileResponse(
                profile.id(),
                profile.username(),
                profile.email(),
                profile.alertMail(),
                profile.superAdmin(),
                profile.keyNeverExpire(),
                profile.keyExpiresAt(),
                profile.hasApiKey(),
                ApiKeyPolicy.isSectionVisible(profile),
                ApiKeyPolicy.isExpired(profile),
                ApiKeyPolicy.canRegenerate(profile)
        );
    }

    public static AccountRolesResponse toRolesResponse(UserProfile profile, List<ProjectRoleOverview> projectRoles) {
        List<ProjectRoleApiDto> projects = projectRoles.stream()
                .map(AccountApiMapper::toProjectRoleDto)
                .toList();
        return new AccountRolesResponse(profile.superAdmin(), projects);
    }

    public static ApiKeyRegenerateResponse toRegenerateResponse(ApiKeyGenerationResult result) {
        return new ApiKeyRegenerateResponse(
                result.plainTextKey(),
                toProfileResponse(result.profile())
        );
    }

    private static ProjectRoleApiDto toProjectRoleDto(ProjectRoleOverview overview) {
        List<ThesaurusRoleApiDto> thesaurusRoles = overview.thesaurusRoles().stream()
                .map(role -> new ThesaurusRoleApiDto(
                        role.thesaurusId(),
                        role.thesaurusName(),
                        role.roleName()
                ))
                .toList();
        return new ProjectRoleApiDto(overview.projectId(), overview.projectName(), thesaurusRoles);
    }
}
