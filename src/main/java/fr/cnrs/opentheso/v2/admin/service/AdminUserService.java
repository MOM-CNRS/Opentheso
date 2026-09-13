package fr.cnrs.opentheso.v2.admin.service;

import fr.cnrs.opentheso.v2.admin.model.CreatedAdminUser;
import fr.cnrs.opentheso.v2.admin.policy.SuperAdminAccessPolicy;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.user.exception.InvalidProfileDataException;
import fr.cnrs.opentheso.v2.user.service.AccountPasswordResetService;
import fr.cnrs.opentheso.v2.user.service.UserLookupService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import fr.cnrs.opentheso.v2.user.validation.PasswordPolicy;
import fr.cnrs.opentheso.v2.user.validation.ProfileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    public static final String CREATION_MODE_DIRECT = "DIRECT";
    public static final String CREATION_MODE_EMAIL = "EMAIL";

    private final UserProfileService userProfileService;
    private final UserLookupService userLookupService;
    private final UserCommandRepository userCommandRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final PasswordEncoder passwordEncoder;
    private final RightsService rightsService;
    private final AccountPasswordResetService accountPasswordResetService;

    @Transactional
    public CreatedAdminUser createUser(CreateUserRequest request) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(request.superAdmin());
        String validUsername = ProfileValidator.requireUsername(request.username());
        String validEmail = ProfileValidator.requireEmail(request.email());
        ensureUsernameAvailable(validUsername);
        ensureEmailAvailable(validEmail);

        boolean emailInvite = isEmailInvite(request.creationMode());
        String institution = StringUtils.trimToNull(request.institution());
        boolean active = !emailInvite && (request.active() == null || request.active());
        int userId;
        if (emailInvite) {
            userId = userCommandRepository.createUser(new UserCommandRepository.CreateUserRequest(
                    validUsername,
                    validEmail,
                    "",
                    request.alertMail(),
                    institution,
                    false,
                    true,
                    false
            ));
            try {
                accountPasswordResetService.requestPasswordReset(validEmail, true);
            } catch (Exception e) {
                log.warn("Utilisateur id={} créé mais l'envoi du mail d'activation a échoué", userId, e);
            }
        } else {
            PasswordPolicy.validate(request.password(), request.passwordConfirmation());
            userId = userCommandRepository.createUser(new UserCommandRepository.CreateUserRequest(
                    validUsername,
                    validEmail,
                    passwordEncoder.encode(request.password()),
                    request.alertMail(),
                    institution,
                    active,
                    false,
                    true
            ));
        }

        assignInitialRole(userId, request.roleId(), request.projectId(), request.limitedOnThesaurus(), request.thesaurusIds());
        assignProjectRoles(userId, request.projectRoles());
        if (request.apiKeyAuthorized()) {
            updateApiKeySettings(
                    request.superAdmin(),
                    userId,
                    true,
                    request.apiKeyNeverExpire(),
                    request.apiKeyExpiresAt()
            );
        }
        log.info("Utilisateur id={} créé par un super-administrateur (mode={})", userId,
                emailInvite ? CREATION_MODE_EMAIL : CREATION_MODE_DIRECT);
        return new CreatedAdminUser(userId, validUsername, validEmail);
    }

    public record ProjectRoleAssignment(int projectId, int roleId) {
    }

    public record CreateUserRequest(
            boolean superAdmin,
            String username,
            String email,
            boolean alertMail,
            Integer roleId,
            Integer projectId,
            boolean limitedOnThesaurus,
            List<String> thesaurusIds,
            String password,
            String passwordConfirmation,
            List<ProjectRoleAssignment> projectRoles,
            boolean apiKeyAuthorized,
            boolean apiKeyNeverExpire,
            LocalDate apiKeyExpiresAt,
            String institution,
            String creationMode,
            Boolean active
    ) {
        public CreateUserRequest {
            if (thesaurusIds == null) {
                thesaurusIds = List.of();
            }
            if (projectRoles == null) {
                projectRoles = List.of();
            }
            if (creationMode == null || creationMode.isBlank()) {
                creationMode = CREATION_MODE_DIRECT;
            }
        }

        /** Compatibilité des appels existants (API / AllUsersBean). */
        public CreateUserRequest(
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
            this(
                    superAdmin,
                    username,
                    email,
                    alertMail,
                    roleId,
                    projectId,
                    limitedOnThesaurus,
                    thesaurusIds,
                    password,
                    passwordConfirmation,
                    List.of(),
                    false,
                    true,
                    null,
                    null,
                    CREATION_MODE_DIRECT,
                    null
            );
        }

        /** Compatibilité create multi-projets (sans institution / mode). */
        public CreateUserRequest(
                boolean superAdmin,
                String username,
                String email,
                boolean alertMail,
                Integer roleId,
                Integer projectId,
                boolean limitedOnThesaurus,
                List<String> thesaurusIds,
                String password,
                String passwordConfirmation,
                List<ProjectRoleAssignment> projectRoles,
                boolean apiKeyAuthorized,
                boolean apiKeyNeverExpire,
                LocalDate apiKeyExpiresAt
        ) {
            this(
                    superAdmin,
                    username,
                    email,
                    alertMail,
                    roleId,
                    projectId,
                    limitedOnThesaurus,
                    thesaurusIds,
                    password,
                    passwordConfirmation,
                    projectRoles,
                    apiKeyAuthorized,
                    apiKeyNeverExpire,
                    apiKeyExpiresAt,
                    null,
                    CREATION_MODE_DIRECT,
                    null
            );
        }

        /** Compatibilité create avec institution / mode (sans active explicite). */
        public CreateUserRequest(
                boolean superAdmin,
                String username,
                String email,
                boolean alertMail,
                Integer roleId,
                Integer projectId,
                boolean limitedOnThesaurus,
                List<String> thesaurusIds,
                String password,
                String passwordConfirmation,
                List<ProjectRoleAssignment> projectRoles,
                boolean apiKeyAuthorized,
                boolean apiKeyNeverExpire,
                LocalDate apiKeyExpiresAt,
                String institution,
                String creationMode
        ) {
            this(
                    superAdmin,
                    username,
                    email,
                    alertMail,
                    roleId,
                    projectId,
                    limitedOnThesaurus,
                    thesaurusIds,
                    password,
                    passwordConfirmation,
                    projectRoles,
                    apiKeyAuthorized,
                    apiKeyNeverExpire,
                    apiKeyExpiresAt,
                    institution,
                    creationMode,
                    null
            );
        }
    }

    /**
     * Mise à jour atomique : profil, super-admin, mot de passe optionnel,
     * affectations projet (remplacement), clé API optionnelle.
     */
    @Transactional
    public void updateUser(UpdateUserRequest request) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(request.superAdmin());
        String validUsername = ProfileValidator.requireUsername(request.username());
        String validEmail = ProfileValidator.requireEmail(request.email());
        var current = userProfileService.getProfile(request.userId());

        if (request.callerId() == request.userId() && current.superAdmin() && !request.makeSuperAdmin()) {
            throw new InvalidProfileDataException("Vous ne pouvez pas retirer votre propre statut super-administrateur.");
        }

        if (!current.username().equalsIgnoreCase(validUsername)
                && userCommandRepository.existsByUsernameIgnoreCase(validUsername)) {
            throw new InvalidProfileDataException("Ce pseudo est déjà utilisé.");
        }
        if (!current.email().equalsIgnoreCase(validEmail)
                && userCommandRepository.existsByMailIgnoreCase(validEmail)) {
            throw new InvalidProfileDataException("Cet email est déjà utilisé.");
        }

        String institution = request.institution() != null
                ? StringUtils.trimToNull(request.institution())
                : userCommandRepository.findInstitution(request.userId());
        boolean active = request.active() != null
                ? request.active()
                : userCommandRepository.isActive(request.userId());
        userCommandRepository.updateUserProfile(
                request.userId(),
                validUsername,
                validEmail,
                request.alertMail(),
                institution,
                active
        );

        if (current.superAdmin() != request.makeSuperAdmin()) {
            userCommandRepository.setSuperAdmin(request.userId(), request.makeSuperAdmin());
            rightsService.invalidate(request.userId());
        }

        if (StringUtils.isNotBlank(request.password()) || StringUtils.isNotBlank(request.passwordConfirmation())) {
            PasswordPolicy.validate(request.password(), request.passwordConfirmation());
            userCommandRepository.updatePassword(request.userId(), passwordEncoder.encode(request.password()));
        }

        if (request.projectRoles() != null) {
            if (request.makeSuperAdmin()) {
                replaceProjectRoles(request.userId(), List.of());
            } else {
                replaceProjectRoles(request.userId(), request.projectRoles());
            }
        }

        if (request.apiKeyAuthorized() != null) {
            updateApiKeySettings(
                    request.superAdmin(),
                    request.userId(),
                    request.apiKeyAuthorized(),
                    request.apiKeyNeverExpire(),
                    request.apiKeyExpiresAt()
            );
        }
    }

    public record UpdateUserRequest(
            boolean superAdmin,
            int userId,
            int callerId,
            String username,
            String email,
            boolean alertMail,
            String institution,
            Boolean active,
            boolean makeSuperAdmin,
            String password,
            String passwordConfirmation,
            List<ProjectRoleAssignment> projectRoles,
            Boolean apiKeyAuthorized,
            boolean apiKeyNeverExpire,
            LocalDate apiKeyExpiresAt
    ) {
    }

    @Transactional
    public void updateUser(
            boolean superAdmin,
            int userId,
            String username,
            String email,
            boolean alertMail
    ) {
        updateUser(new UpdateUserRequest(
                superAdmin,
                userId,
                -1,
                username,
                email,
                alertMail,
                null,
                null,
                userProfileService.getProfile(userId).superAdmin(),
                null,
                null,
                null,
                null,
                true,
                null
        ));
    }

    /**
     * Met à jour le profil puis synchronise le flag super-administrateur.
     */
    @Transactional
    public void updateUser(
            boolean superAdmin,
            int userId,
            String username,
            String email,
            boolean alertMail,
            boolean makeSuperAdmin
    ) {
        updateUser(new UpdateUserRequest(
                superAdmin,
                userId,
                -1,
                username,
                email,
                alertMail,
                null,
                null,
                makeSuperAdmin,
                null,
                null,
                null,
                null,
                true,
                null
        ));
    }

    @Transactional
    public void setSuperAdmin(boolean callerIsSuperAdmin, int userId, boolean makeSuperAdmin) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(callerIsSuperAdmin);
        userLookupService.requireEntity(userId);
        userCommandRepository.setSuperAdmin(userId, makeSuperAdmin);
        rightsService.invalidate(userId);
    }

    @Transactional
    public void updatePassword(boolean superAdmin, int userId, String password, String confirmation) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        userLookupService.requireEntity(userId);
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
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        userLookupService.requireEntity(userId);

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
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        if (callerId == userId) {
            throw new InvalidProfileDataException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        userLookupService.requireEntity(userId);
        userCommandRepository.deleteUserCascade(userId);
        log.info("Utilisateur id={} supprimé par le super-administrateur id={}", userId, callerId);
    }

    @Transactional(readOnly = true)
    public List<ProjectRoleAssignment> listProjectRoles(boolean superAdmin, int userId) {
        SuperAdminAccessPolicy.requireResolvedSuperAdmin(superAdmin);
        userLookupService.requireEntity(userId);
        return projectMembershipRepository.findProjectRolesForUser(userId).stream()
                .map(row -> new ProjectRoleAssignment(row.projectId(), row.roleId()))
                .toList();
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

    private void assignProjectRoles(int userId, List<ProjectRoleAssignment> projectRoles) {
        if (projectRoles == null || projectRoles.isEmpty()) {
            return;
        }
        for (ProjectRoleAssignment assignment : projectRoles) {
            if (assignment == null) {
                continue;
            }
            validateProjectRole(assignment.roleId());
            if (assignment.projectId() <= 0) {
                throw new InvalidProjectDataException("Un projet est requis pour attribuer un rôle.");
            }
            projectMembershipRepository.assignProjectRole(userId, assignment.roleId(), assignment.projectId());
            projectMembershipRepository.deleteAllLimitedRoles(userId, assignment.projectId());
        }
        rightsService.invalidate(userId);
    }

    private void replaceProjectRoles(int userId, List<ProjectRoleAssignment> desired) {
        List<ProjectMembershipRepository.ProjectRoleRow> current =
                projectMembershipRepository.findProjectRolesForUser(userId);
        Set<Integer> desiredProjectIds = new HashSet<>();
        if (desired != null) {
            for (ProjectRoleAssignment assignment : desired) {
                if (assignment == null) {
                    continue;
                }
                validateProjectRole(assignment.roleId());
                if (assignment.projectId() <= 0) {
                    throw new InvalidProjectDataException("Un projet est requis pour attribuer un rôle.");
                }
                if (!desiredProjectIds.add(assignment.projectId())) {
                    throw new InvalidProjectDataException("Un même projet ne peut être affecté qu'une seule fois.");
                }
                projectMembershipRepository.assignProjectRole(userId, assignment.roleId(), assignment.projectId());
                projectMembershipRepository.deleteAllLimitedRoles(userId, assignment.projectId());
            }
        }
        for (ProjectMembershipRepository.ProjectRoleRow row : current) {
            if (!desiredProjectIds.contains(row.projectId())) {
                projectMembershipRepository.deleteAllLimitedRoles(userId, row.projectId());
                projectMembershipRepository.deleteProjectRole(userId, row.projectId());
            }
        }
        rightsService.invalidate(userId);
    }

    private static void validateProjectRole(int roleId) {
        if (roleId != ProjectAccessPolicy.ROLE_ADMIN
                && roleId != ProjectAccessPolicy.ROLE_MANAGER
                && roleId != ProjectAccessPolicy.ROLE_CONTRIBUTOR) {
            throw new InvalidProjectDataException("Rôle projet invalide : " + roleId);
        }
    }

    private static boolean isEmailInvite(String creationMode) {
        return CREATION_MODE_EMAIL.equalsIgnoreCase(StringUtils.trimToEmpty(creationMode));
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
