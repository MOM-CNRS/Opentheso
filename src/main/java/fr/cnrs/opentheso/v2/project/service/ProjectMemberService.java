package fr.cnrs.opentheso.v2.project.service;

import fr.cnrs.opentheso.v2.user.service.AccountPasswordResetService;
import fr.cnrs.opentheso.v2.project.exception.InvalidProjectDataException;
import fr.cnrs.opentheso.v2.project.exception.ProjectAccessDeniedException;
import fr.cnrs.opentheso.v2.project.model.CreatedProjectMember;
import fr.cnrs.opentheso.v2.project.model.UserSearchResult;
import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.shared.repository.ProjectAdminQueryRepository;
import fr.cnrs.opentheso.v2.shared.repository.ProjectMembershipRepository;
import fr.cnrs.opentheso.v2.shared.repository.UserCommandRepository;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.user.exception.UserNotFoundException;
import fr.cnrs.opentheso.v2.user.service.UserLookupService;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import fr.cnrs.opentheso.v2.user.validation.PasswordPolicy;
import fr.cnrs.opentheso.v2.user.validation.ProfileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private static final int SEARCH_LIMIT = 20;

    private final UserProfileService userProfileService;
    private final UserLookupService userLookupService;
    private final ProjectLookupService projectLookupService;
    private final ProjectAdminQueryRepository projectAdminQueryRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final UserCommandRepository userCommandRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountPasswordResetService accountPasswordResetService;
    private final RightsService rightsService;

    @Transactional(readOnly = true)
    public List<UserSearchResult> searchUsers(int callerId, boolean superAdmin, int projectId, String username) {
        requireProjectAdmin(callerId, superAdmin, projectId);
        String pattern = StringUtils.trimToNull(username);
        if (pattern == null) {
            return List.of();
        }
        return userCommandRepository.searchByUsernameLike(pattern, SEARCH_LIMIT).stream()
                .map(row -> new UserSearchResult(row.userId(), row.username(), row.email()))
                .toList();
    }

    @Transactional
    public CreatedProjectMember createMember(
            int callerId,
            boolean superAdmin,
            int projectId,
            String username,
            String email,
            String institution,
            boolean alertMail,
            int roleId,
            boolean limitedOnThesaurus,
            List<String> thesaurusIds,
            String password,
            String passwordConfirmation,
            String creationMode
    ) {
        AdminContext context = requireProjectAdmin(callerId, superAdmin, projectId);
        validateAssignableRole(context, roleId);
        String validUsername = ProfileValidator.requireUsername(username);
        String validEmail = ProfileValidator.requireEmail(email);
        ensureUsernameAvailable(validUsername);
        ensureEmailAvailable(validEmail);

        if (limitedOnThesaurus) {
            requireThesaurusIds(projectId, thesaurusIds);
        }

        int userId = createUserAccount(
                validUsername,
                validEmail,
                institution,
                alertMail,
                password,
                passwordConfirmation,
                creationMode
        );
        assignMembership(userId, roleId, projectId, limitedOnThesaurus, thesaurusIds);
        log.info("Utilisateur id={} créé et ajouté au projet id={} par l'utilisateur id={}", userId, projectId, callerId);
        return new CreatedProjectMember(userId, validUsername, validEmail);
    }

    @Transactional
    public void addExistingMember(int callerId, boolean superAdmin, int projectId, int userId, int roleId) {
        AdminContext context = requireProjectAdmin(callerId, superAdmin, projectId);
        validateAssignableRole(context, roleId);
        ensureUserExists(userId);
        if (callerId == userId) {
            throw new InvalidProjectDataException("Vous ne pouvez pas vous ajouter vous-même au projet.");
        }
        if (projectAdminQueryRepository.isProjectAccessible(userId, projectId)) {
            throw new InvalidProjectDataException("Cet utilisateur est déjà membre de ce projet.");
        }
        projectMembershipRepository.assignProjectRole(userId, roleId, projectId);
        rightsService.invalidate(userId);
        log.info("Utilisateur id={} ajouté au projet id={} par l'utilisateur id={}", userId, projectId, callerId);
    }

    @Transactional
    public void updateMemberRole(
            int callerId,
            boolean superAdmin,
            int projectId,
            int userId,
            int roleId,
            boolean limitedOnThesaurus,
            List<String> thesaurusIds
    ) {
        AdminContext context = requireProjectAdmin(callerId, superAdmin, projectId);
        validateAssignableRole(context, roleId);
        ensureUserExists(userId);
        if (limitedOnThesaurus) {
            requireThesaurusIds(projectId, thesaurusIds);
            projectMembershipRepository.deleteProjectRole(userId, projectId);
            projectMembershipRepository.replaceLimitedRoles(userId, roleId, projectId, thesaurusIds);
        } else {
            projectMembershipRepository.deleteAllLimitedRoles(userId, projectId);
            projectMembershipRepository.deleteProjectRole(userId, projectId);
            projectMembershipRepository.assignProjectRole(userId, roleId, projectId);
        }
        rightsService.invalidate(userId);
        log.info("Rôle mis à jour pour l'utilisateur id={} sur le projet id={}", userId, projectId);
    }

    @Transactional
    public void updateLimitedMemberRole(
            int callerId,
            boolean superAdmin,
            int projectId,
            int userId,
            int oldRoleId,
            int newRoleId,
            String thesaurusId,
            boolean limitedOnThesaurus
    ) {
        AdminContext context = requireProjectAdmin(callerId, superAdmin, projectId);
        validateAssignableRole(context, newRoleId);
        ensureUserExists(userId);
        if (limitedOnThesaurus) {
            if (!projectMembershipRepository.isThesaurusInProject(thesaurusId, projectId)) {
                throw new InvalidProjectDataException("Le thésaurus n'appartient pas à ce projet.");
            }
            projectMembershipRepository.deleteLimitedRole(userId, oldRoleId, projectId, thesaurusId);
            projectMembershipRepository.assignLimitedRole(userId, newRoleId, projectId, thesaurusId);
        } else {
            projectMembershipRepository.deleteAllLimitedRoles(userId, projectId);
            projectMembershipRepository.assignProjectRole(userId, newRoleId, projectId);
        }
        rightsService.invalidate(userId);
        log.info("Rôle limité mis à jour pour l'utilisateur id={} sur le thésaurus {}", userId, thesaurusId);
    }

    @Transactional
    public void removeMember(int callerId, boolean superAdmin, int projectId, int userId) {
        requireProjectAdmin(callerId, superAdmin, projectId);
        ensureUserExists(userId);
        if (callerId == userId) {
            throw new InvalidProjectDataException("Vous ne pouvez pas vous retirer vous-même du projet.");
        }
        projectMembershipRepository.deleteProjectRole(userId, projectId);
        projectMembershipRepository.deleteAllLimitedRoles(userId, projectId);
        rightsService.invalidate(userId);
        log.info("Utilisateur id={} retiré du projet id={}", userId, projectId);
    }

    @Transactional
    public void removeLimitedRole(
            int callerId,
            boolean superAdmin,
            int projectId,
            int userId,
            int roleId,
            String thesaurusId
    ) {
        requireProjectAdmin(callerId, superAdmin, projectId);
        ensureUserExists(userId);
        projectMembershipRepository.deleteLimitedRole(userId, roleId, projectId, thesaurusId);
        rightsService.invalidate(userId);
        log.info("Rôle limité supprimé pour l'utilisateur id={} sur le thésaurus {}", userId, thesaurusId);
    }

    @Transactional(readOnly = true)
    public String getMemberInstitution(int callerId, boolean superAdmin, int projectId, int userId) {
        requireProjectAdmin(callerId, superAdmin, projectId);
        ensureUserExists(userId);
        return userCommandRepository.findInstitution(userId);
    }

    @Transactional
    public void updateMemberProfile(
            int callerId,
            boolean superAdmin,
            int projectId,
            int userId,
            String username,
            String email,
            boolean alertMail,
            String institution,
            boolean active
    ) {
        requireProjectAdmin(callerId, superAdmin, projectId);
        String validUsername = ProfileValidator.requireUsername(username);
        String validEmail = ProfileValidator.requireEmail(email);
        var current = userProfileService.getProfile(userId);
        if (!current.username().equalsIgnoreCase(validUsername)
                && userCommandRepository.existsByUsernameIgnoreCase(validUsername)) {
            throw new InvalidProjectDataException("Ce pseudo est déjà utilisé.");
        }
        if (!current.email().equalsIgnoreCase(validEmail)
                && userCommandRepository.existsByMailIgnoreCase(validEmail)) {
            throw new InvalidProjectDataException("Cet email est déjà utilisé.");
        }
        userCommandRepository.updateUserProfile(userId, validUsername, validEmail, alertMail, institution, active);
        log.info("Profil mis à jour pour l'utilisateur id={} par l'administrateur id={}", userId, callerId);
    }

    @Transactional
    public void setMemberPassword(
            int callerId,
            boolean superAdmin,
            int projectId,
            int userId,
            String password,
            String confirmation
    ) {
        requireProjectAdmin(callerId, superAdmin, projectId);
        ensureUserExists(userId);
        PasswordPolicy.validate(password, confirmation);
        userCommandRepository.updatePassword(userId, passwordEncoder.encode(password));
        log.info("Mot de passe mis à jour pour l'utilisateur id={} par l'administrateur id={}", userId, callerId);
    }

    @Transactional
    public void moveThesaurus(
            int callerId,
            boolean superAdmin,
            int projectId,
            String thesaurusId,
            int targetProjectId
    ) {
        requireProjectAdmin(callerId, superAdmin, projectId);
        projectLookupService.requireEntity(targetProjectId);
        if (!projectMembershipRepository.isThesaurusInProject(thesaurusId, projectId)) {
            throw new InvalidProjectDataException("Le thésaurus n'appartient pas à ce projet.");
        }
        if (!superAdmin) {
            projectLookupService.requireAccessibleProject(callerId, false, targetProjectId);
            Optional<Integer> targetRole = projectAdminQueryRepository.findCallerRoleOnProject(callerId, targetProjectId);
            if (!ProjectAccessPolicy.isProjectAdmin(false, targetRole.orElse(null))) {
                throw new ProjectAccessDeniedException();
            }
        }
        projectMembershipRepository.moveThesaurus(thesaurusId, targetProjectId);
        log.info("Thésaurus {} déplacé du projet id={} vers id={}", thesaurusId, projectId, targetProjectId);
    }

    private int createUserAccount(
            String username,
            String email,
            String institution,
            boolean alertMail,
            String password,
            String passwordConfirmation,
            String creationMode
    ) {
        if ("EMAIL".equalsIgnoreCase(creationMode)) {
            int userId = userCommandRepository.createUser(
                    username, email, "", alertMail, institution, false, true, false
            );
            try {
                accountPasswordResetService.requestPasswordReset(email, true);
            } catch (Exception e) {
                log.warn("Utilisateur id={} créé mais l'envoi du mail a échoué", userId, e);
            }
            return userId;
        }
        PasswordPolicy.validate(password, passwordConfirmation);
        return userCommandRepository.createUser(
                username,
                email,
                passwordEncoder.encode(password),
                alertMail,
                institution,
                true,
                false,
                true
        );
    }

    private void assignMembership(
            int userId,
            int roleId,
            int projectId,
            boolean limitedOnThesaurus,
            List<String> thesaurusIds
    ) {
        if (limitedOnThesaurus) {
            projectMembershipRepository.replaceLimitedRoles(userId, roleId, projectId, thesaurusIds);
        } else {
            projectMembershipRepository.assignProjectRole(userId, roleId, projectId);
        }
        rightsService.invalidate(userId);
    }

    private AdminContext requireProjectAdmin(int callerId, boolean superAdmin, int projectId) {
        projectLookupService.requireAccessibleProject(callerId, superAdmin, projectId);
        if (superAdmin) {
            return new AdminContext(ProjectAccessPolicy.ROLE_SUPER_ADMIN, ProjectAccessPolicy.ROLE_SUPER_ADMIN);
        }
        int callerRoleId = projectAdminQueryRepository.findCallerRoleOnProject(callerId, projectId)
                .orElseThrow(ProjectAccessDeniedException::new);
        if (!ProjectAccessPolicy.isProjectAdmin(false, callerRoleId)) {
            throw new ProjectAccessDeniedException();
        }
        return new AdminContext(
                callerRoleId,
                ProjectAccessPolicy.minAssignableRoleId(false, callerRoleId)
        );
    }

    private static void validateAssignableRole(AdminContext context, int roleId) {
        if (roleId < context.minAssignableRoleId()) {
            throw new ProjectAccessDeniedException();
        }
        if (roleId == ProjectAccessPolicy.ROLE_SUPER_ADMIN && context.callerRoleId() != ProjectAccessPolicy.ROLE_SUPER_ADMIN) {
            throw new ProjectAccessDeniedException();
        }
    }

    private void ensureUserExists(int userId) {
        try {
            userLookupService.requireEntity(userId);
        } catch (UserNotFoundException e) {
            throw new InvalidProjectDataException("Utilisateur introuvable.");
        }
    }

    private void ensureUsernameAvailable(String username) {
        if (userCommandRepository.existsByUsernameIgnoreCase(username)) {
            throw new InvalidProjectDataException("Ce pseudo est déjà utilisé.");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (userCommandRepository.existsByMailIgnoreCase(email)) {
            throw new InvalidProjectDataException("Cet email est déjà utilisé.");
        }
    }

    private void requireThesaurusIds(int projectId, List<String> thesaurusIds) {
        if (CollectionUtils.isEmpty(thesaurusIds)) {
            throw new InvalidProjectDataException("Au moins un thésaurus est requis pour un rôle limité.");
        }
        var missing = projectAdminQueryRepository.findThesauriNotInProject(thesaurusIds, projectId);
        if (!missing.isEmpty()) {
            throw new InvalidProjectDataException("Les thésaurus suivants n'appartiennent pas à ce projet : " + missing);
        }
    }

    private record AdminContext(int callerRoleId, int minAssignableRoleId) {
    }
}
