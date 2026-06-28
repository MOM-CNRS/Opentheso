package fr.cnrs.opentheso.v2.shared.auth;

import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.repository.UserRoleQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCapabilityService {

    private final UserProfileService userProfileService;
    private final UserRoleQueryRepository userRoleQueryRepository;

    @Transactional(readOnly = true)
    public SessionUser loadSessionUser(int userId) {
        var profile = userProfileService.getProfile(userId);
        if (profile.superAdmin()) {
            return new SessionUser(
                    userId,
                    profile.username(),
                    profile.email(),
                    true,
                    true,
                    true,
                    true
            );
        }

        int bestRole = userRoleQueryRepository.findBestRoleId(userId)
                .orElse(ProjectAccessPolicy.ROLE_CONTRIBUTOR + 1);

        boolean projectAdmin = bestRole < ProjectAccessPolicy.ROLE_MANAGER;
        boolean contributor = bestRole <= ProjectAccessPolicy.ROLE_CONTRIBUTOR;
        boolean manager = bestRole <= ProjectAccessPolicy.ROLE_MANAGER;

        return new SessionUser(
                userId,
                profile.username(),
                profile.email(),
                false,
                projectAdmin,
                contributor,
                manager
        );
    }
}
