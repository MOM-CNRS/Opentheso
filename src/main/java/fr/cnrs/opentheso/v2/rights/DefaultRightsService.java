package fr.cnrs.opentheso.v2.rights;

import fr.cnrs.opentheso.v2.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.v2.setting.policy.ThesaurusAccessPolicy;
import fr.cnrs.opentheso.v2.shared.exception.ModuleAccessDeniedException;
import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implémentation unique du module de droits : décisions + lecture via cache.
 * Seuils unifiés pour projet / thésaurus / toolbox / concepts.
 */
@Service
@RequiredArgsConstructor
public class DefaultRightsService implements RightsService {

    private final UserRightsCache userRightsCache;

    @Override
    public SessionUser capabilities(int userId) {
        return userRightsCache.getSessionUser(userId);
    }

    @Override
    public Optional<Integer> roleOnThesaurus(int userId, String thesaurusId) {
        return userRightsCache.getEffectiveRoleOnThesaurus(userId, thesaurusId);
    }

    @Override
    public boolean can(Integer userId, Permission permission) {
        return can(userId, permission, AuthTarget.none());
    }

    @Override
    public boolean can(Integer userId, Permission permission, AuthTarget target) {
        if (userId == null || permission == null) {
            return false;
        }
        AuthTarget safeTarget = target == null ? AuthTarget.none() : target;
        return evaluate(userRightsCache.getSessionUser(userId), permission, safeTarget);
    }

    @Override
    public boolean can(UserSession userSession, Permission permission) {
        return can(userSession, permission, AuthTarget.none());
    }

    @Override
    public boolean can(UserSession userSession, Permission permission, AuthTarget target) {
        if (userSession == null || !userSession.isLoggedIn()) {
            return false;
        }
        return can(userSession.getCurrentUserId(), permission, target);
    }

    @Override
    public void require(Integer userId, Permission permission, AuthTarget target) {
        if (!can(userId, permission, target)) {
            throw new ModuleAccessDeniedException(permission == null ? "rights" : permission.name().toLowerCase());
        }
    }

    @Override
    public void invalidate(int userId) {
        userRightsCache.invalidate(userId);
    }

    private boolean evaluate(SessionUser user, Permission permission, AuthTarget target) {
        return switch (permission) {
            case SUPER_ADMIN -> user.superAdmin();
            case MANAGE_PROJECT -> canManageProject(user, target);
            case MANAGE_THESAURUS, WRITE_THESAURUS -> canManageThesaurus(user, target);
            case MANAGE_THESAURUS_STRUCTURE, MUTATE_CONCEPT_STRUCTURE ->
                    hasThesaurusRoleAtMost(user, target, ProjectAccessPolicy.ROLE_MANAGER);
            case CONTRIBUTE_ON_THESAURUS, MUTATE_CONCEPT, ACCESS_CANDIDAT ->
                    hasThesaurusRoleAtMost(user, target, ProjectAccessPolicy.ROLE_CONTRIBUTOR);
            case TOOLBOX_EDITION, TOOLBOX_MAINTENANCE -> user.superAdmin() || user.projectAdmin();
            case TOOLBOX_STATISTICS -> user.superAdmin() || user.manager();
            case TOOLBOX_FLAGS -> user.superAdmin();
            case ACCESS_GRAPH, ACCESS_WORKSHOP -> true;
        };
    }

    private boolean canManageProject(SessionUser user, AuthTarget target) {
        if (user.superAdmin()) {
            return true;
        }
        if (!target.hasProject()) {
            return user.projectAdmin();
        }
        return userRightsCache.getRoleOnProject(user.userId(), target.projectId())
                .map(roleId -> ProjectAccessPolicy.isProjectAdmin(false, roleId))
                .orElse(false);
    }

    private boolean canManageThesaurus(SessionUser user, AuthTarget target) {
        if (user.superAdmin()) {
            return true;
        }
        if (!target.hasThesaurus()) {
            return user.projectAdmin();
        }
        return userRightsCache.getEffectiveRoleOnThesaurus(user.userId(), target.thesaurusId())
                .map(roleId -> ThesaurusAccessPolicy.isThesaurusAdmin(false, roleId))
                .orElse(false);
    }

    /**
     * Rôle effectif sur le thésaurus cible ≤ maxRoleId (1=admin … 4=contributor).
     * Sans thésaurus : capacités session (meilleur rôle connu) — fallback UI globale.
     */
    private boolean hasThesaurusRoleAtMost(SessionUser user, AuthTarget target, int maxRoleId) {
        if (user.superAdmin()) {
            return true;
        }
        if (!target.hasThesaurus()) {
            if (maxRoleId >= ProjectAccessPolicy.ROLE_CONTRIBUTOR) {
                return user.contributor() || user.manager() || user.projectAdmin();
            }
            if (maxRoleId >= ProjectAccessPolicy.ROLE_MANAGER) {
                return user.manager() || user.projectAdmin();
            }
            return user.projectAdmin();
        }
        return userRightsCache.getEffectiveRoleOnThesaurus(user.userId(), target.thesaurusId())
                .map(roleId -> roleId <= maxRoleId)
                .orElse(false);
    }
}
