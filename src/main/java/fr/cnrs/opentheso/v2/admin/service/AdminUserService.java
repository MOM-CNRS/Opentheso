package fr.cnrs.opentheso.v2.admin.service;

import fr.cnrs.opentheso.v2.admin.model.CreatedAdminUser;
import fr.cnrs.opentheso.v2.admin.policy.SuperAdminAccessPolicy;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.service.UserLookupService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import fr.cnrs.opentheso.v2.user.validation.PasswordPolicy;
import fr.cnrs.opentheso.v2.user.validation.ProfileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserProfileService userProfileService;
    private final UserLookupService userLookupService;
    private final UserCommandRepository userCommandRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RightsService rightsService;

    @Transactional
    public CreatedAdminUser createUser(
            boolean superAdmin,
            String username,
            String email,
            boolean alertMail,
            Integer roleId,
            Integer projectId,
            boolean limitedOnThesaurus,
            List<String> thesaurusIds,
            String password,
            String passwordConfirmation
    ) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        String validUsername = ProfileValidator.requireUsername(username);
        String validEmail = ProfileValidator.requireEmail(email);
        ensureUsernameAvailable(validUsername);
        ensureEmailAvailable(validEmail);

        PasswordPolicy.validate(password, passwordConfirmation);
        int userId = userCommandRepository.createUser(
                validUsername,
                validEmail,
                passwordEncoder.encode(password),
                alertMail,
                null,
                true,
                false,
                true
        );
        assignInitialRole(userId, roleId, projectId, limitedOnThesaurus, thesaurusIds);
        log.info("Utilisateur id={} créé par un super-administrateur", userId);
        return new CreatedAdminUser(userId, validUsername, validEmail);
    }

    @Transactional
    public void updateUser(
            boolean superAdmin,
            int userId,
            String username,
            String email,
            boolean alertMail
    ) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        String validUsername = ProfileValidator.requireUsername(username);
        String validEmail = ProfileValidator.requireEmail(email);
        var current = userProfileService.getProfile(userId);
        if (!current.username().equalsIgnoreCase(validUsername)
                && userCommandRepository.existsByUsernameIgnoreCase(validUsername)) {
            throw new InvalidProfileDataException("Ce pseudo est déjà utilisé.");
        }
        if (!current.email().equalsIgnoreCase(validEmail)
                && userCommandRepository.existsByMailIgnoreCase(validEmail)) {
            throw new InvalidProfileDataException("Cet email est déjà utilisé.");
        }
        userCommandRepository.updateUserProfile(userId, validUsername, validEmail, alertMail, null, true);
    }

    @Transactional
    public void updatePassword(boolean superAdmin, int userId, String password, String confirmation) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        userLookupService.requireEntity(userId); // existence check only — no profile needed
        PasswordPolicy.validate(password, confirmation);
        userCommandRepository.updatePassword(userId, passwordEncoder.encode(password));
    }

    @Transactional
    public void updateApiKeySettings(
            boolean superAdmin,
            int userId,
            boolean authorized,
            boolean keyNeverExpire,
            LocalDate keyExpiresAt
    ) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        userLookupService.requireEntity(userId);

        // L'autorisation repose sur key_never_expire OU key_expires_at (ApiKeyPolicy).
        boolean neverExpire;
        LocalDate expiresAt;
        if (!authorized) {
            neverExpire = false;
            expiresAt = null;
        } else if (keyNeverExpire || keyExpiresAt == null) {
            neverExpire = true;
            expiresAt = null;
        } else {
            neverExpire = false;
            expiresAt = keyExpiresAt;
        }

        userCommandRepository.updateApiKeySettings(userId, authorized, neverExpire, expiresAt);
    }

    @Transactional
    public void deleteUser(boolean superAdmin, int userId, int callerId) {
        SuperAdminAccessPolicy.requireSuperAdmin(superAdmin);
        if (callerId == userId) {
            throw new InvalidProfileDataException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        userLookupService.requireEntity(userId);
        userCommandRepository.deleteUserCascade(userId);
        log.info("Utilisateur id={} supprimé par le super-administrateur id={}", userId, callerId);
    }

    private void assignInitialRole(
            int userId,
            Integer roleId,
            Integer projectId,
            boolean limitedOnThesaurus,
            List<String> thesaurusIds
    ) {
        if (roleId == null) {
            return;
        }
        if (roleId == ProjectAccessPolicy.ROLE_SUPER_ADMIN) {
            userCommandRepository.setSuperAdmin(userId, true);
            rightsService.invalidate(userId);
            return;
        }
        if (projectId == null) {
            throw new InvalidProjectDataException("Un projet est requis pour attribuer un rôle.");
        }
        if (limitedOnThesaurus) {
            if (thesaurusIds == null || thesaurusIds.isEmpty()) {
                throw new InvalidProjectDataException("Au moins un thésaurus est requis pour un rôle limité.");
            }
            var missing = projectAdminQueryRepository.findThesauriNotInProject(thesaurusIds, projectId);
            if (!missing.isEmpty()) {
                throw new InvalidProjectDataException("Les thésaurus suivants n'appartiennent pas au projet : " + missing);
            }
            projectMembershipRepository.replaceLimitedRoles(userId, roleId, projectId, thesaurusIds);
            rightsService.invalidate(userId);
            return;
        }
        projectMembershipRepository.assignProjectRole(userId, roleId, projectId);
        rightsService.invalidate(userId);
    }

    private void ensureUsernameAvailable(String username) {
        if (userCommandRepository.existsByUsernameIgnoreCase(username)) {
            throw new InvalidProfileDataException("Ce pseudo est déjà utilisé.");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (userCommandRepository.existsByMailIgnoreCase(email)) {
            throw new InvalidProfileDataException("Cet email est déjà utilisé.");
        }
    }
}
