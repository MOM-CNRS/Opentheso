package fr.cnrs.opentheso.v2.rights;

import fr.cnrs.opentheso.v2.shared.session.SessionUser;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;

import java.util.Optional;

/**
 * Module centralisé de gestion des droits V2.
 * <p>
 * Toutes les décisions d'autorisation doivent passer par cette API.
 * Les droits sont mis en cache ({@link UserRightsCache}) : pas de rechargement DB
 * à chaque contrôle ; TTL configurable + invalidation à la modification des rôles.
 */
public interface RightsService {

    SessionUser capabilities(int userId);

    Optional<Integer> roleOnThesaurus(int userId, String thesaurusId);

    boolean can(Integer userId, Permission permission);

    boolean can(Integer userId, Permission permission, AuthTarget target);

    boolean can(UserSession userSession, Permission permission);

    boolean can(UserSession userSession, Permission permission, AuthTarget target);

    default boolean canOnThesaurus(Integer userId, Permission permission, String thesaurusId) {
        return can(userId, permission, AuthTarget.thesaurus(thesaurusId));
    }

    default boolean canOnProject(Integer userId, Permission permission, int projectId) {
        return can(userId, permission, AuthTarget.project(projectId));
    }

    void require(Integer userId, Permission permission, AuthTarget target);

    void invalidate(int userId);
}
