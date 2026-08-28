package fr.cnrs.opentheso.v2.user.service;

import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.mapper.UserProfileMapper;
import fr.cnrs.opentheso.v2.user.model.ProfileWithRoles;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.shared.persistence.UserEntity;
import fr.cnrs.opentheso.v2.shared.repository.UserProfileRepository;
import fr.cnrs.opentheso.v2.user.validation.ProfileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserLookupService userLookupService;
    private final UserRoleOverviewService userRoleOverviewService;

    @Transactional(readOnly = true)
    public UserProfile getProfile(int userId) {
        log.debug("Chargement du profil v2 pour l'utilisateur id={}", userId);
        return UserProfileMapper.toProfile(userLookupService.requireEntity(userId));
    }

    @Transactional(readOnly = true)
    public ProfileWithRoles getProfileWithRoles(int userId) {
        UserProfile profile = getProfile(userId);
        return new ProfileWithRoles(
                profile,
                userRoleOverviewService.loadProjectRoles(userId, profile.superAdmin())
        );
    }

    @Transactional
    public UserProfile updateUsername(int userId, String username) {
        String validUsername = ProfileValidator.requireUsername(username);
        UserEntity user = userLookupService.requireEntity(userId);
        ensureUsernameAvailable(validUsername, userId);
        user.setUsername(validUsername);
        log.debug("Mise à jour du pseudo v2 pour l'utilisateur id={}", userId);
        return saveEntity(user);
    }

    @Transactional
    public UserProfile updateEmail(int userId, String email) {
        String validEmail = ProfileValidator.requireEmail(email);
        UserEntity user = userLookupService.requireEntity(userId);
        ensureEmailAvailable(validEmail, userId);
        user.setMail(validEmail);
        log.debug("Mise à jour de l'email v2 pour l'utilisateur id={}", userId);
        return saveEntity(user);
    }

    @Transactional
    public UserProfile updateAlertMail(int userId, boolean alertMail) {
        UserEntity user = userLookupService.requireEntity(userId);
        user.setAlertMail(alertMail);
        log.debug("Mise à jour de l'alerte email v2 pour l'utilisateur id={}", userId);
        return saveEntity(user);
    }

    @Transactional
    public UserProfile updateIdentity(int userId, String username, String email) {
        String validUsername = ProfileValidator.requireUsername(username);
        String validEmail = ProfileValidator.requireEmail(email);
        UserEntity user = userLookupService.requireEntity(userId);
        ensureUsernameAvailable(validUsername, userId);
        ensureEmailAvailable(validEmail, userId);
        user.setUsername(validUsername);
        user.setMail(validEmail);
        log.debug("Mise à jour de l'identité v2 pour l'utilisateur id={}", userId);
        return saveEntity(user);
    }

    @Transactional
    UserProfile saveEntity(UserEntity user) {
        return UserProfileMapper.toProfile(userProfileRepository.save(user));
    }

    private void ensureUsernameAvailable(String username, int userId) {
        if (userProfileRepository.existsByUsernameIgnoreCaseExcludingId(username, userId)) {
            throw new InvalidProfileDataException("Ce pseudo est déjà utilisé.");
        }
    }

    private void ensureEmailAvailable(String email, int userId) {
        if (userProfileRepository.existsByMailIgnoreCaseExcludingId(email, userId)) {
            throw new InvalidProfileDataException("Cet email est déjà utilisé.");
        }
    }
}
