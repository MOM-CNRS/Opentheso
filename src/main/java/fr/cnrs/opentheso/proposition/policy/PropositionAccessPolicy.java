package fr.cnrs.opentheso.proposition.policy;

import fr.cnrs.opentheso.project.policy.ProjectAccessPolicy;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.shared.ui.UserSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Façade propositions : décisions via {@link RightsService} + préférence « suggestion ».
 */
@Component
@RequiredArgsConstructor
public class PropositionAccessPolicy {

    private final RightsService rightsService;
    private final ThesaurusPreferenceService thesaurusPreferenceService;

    public boolean isSuggestionEnabled(String thesaurusId, String workLang) {
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(
                thesaurusId, StringUtils.defaultIfBlank(workLang, "fr"));
        return preferences != null && preferences.suggestion();
    }

    /**
     * Soumission publique (comme le legacy) : préférence ON, sans exiger de login.
     */
    public boolean canSubmit(UserSession userSession, String thesaurusId, String workLang) {
        return canSubmit(thesaurusId, workLang);
    }

    /** Variante API / sans session : seule la préférence « suggestion » compte. */
    public boolean canSubmit(String thesaurusId, String workLang) {
        return isSuggestionEnabled(thesaurusId, workLang);
    }

    /**
     * Board / badge : super-utilisateur, ou rôle autre que contributeur sur ce thésaurus
     * (admin / gestionnaire).
     */
    public boolean canAccessBoard(UserSession userSession, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId) || !isLoggedIn(userSession)) {
            return false;
        }
        if (userSession.isSuperAdmin()) {
            return true;
        }
        Integer userId = userSession.getCurrentUserId();
        if (userId == null || userId <= 0) {
            return false;
        }
        return hasNonContributorRoleOnThesaurus(userId, thesaurusId);
    }

    public boolean canAccessBoard(int userId, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId) || userId <= 0) {
            return false;
        }
        if (rightsService.can(userId, Permission.SUPER_ADMIN)) {
            return true;
        }
        return hasNonContributorRoleOnThesaurus(userId, thesaurusId);
    }

    private boolean hasNonContributorRoleOnThesaurus(int userId, String thesaurusId) {
        return rightsService.roleOnThesaurus(userId, thesaurusId)
                .map(roleId -> roleId > 0 && roleId < ProjectAccessPolicy.ROLE_CONTRIBUTOR)
                .orElse(false);
    }

    /** Ouvrir une revue : board + suggestions activées. */
    public boolean canReview(UserSession userSession, String thesaurusId, String workLang) {
        return canAccessBoard(userSession, thesaurusId) && isSuggestionEnabled(thesaurusId, workLang);
    }

    public boolean canReview(int userId, String thesaurusId, String workLang) {
        return canAccessBoard(userId, thesaurusId) && isSuggestionEnabled(thesaurusId, workLang);
    }

    /**
     * L'auteur d'une proposition peut la traiter lui-même s'il est super-administrateur
     * ou administrateur du projet du thésaurus. Un gestionnaire auteur doit laisser décider un autre.
     */
    public boolean canDecideOwn(UserSession userSession, String thesaurusId) {
        return isLoggedIn(userSession) && rightsService.isAdminOfThesaurusProject(userSession, thesaurusId);
    }

    public boolean canDecideOwn(int userId, String thesaurusId) {
        if (userId <= 0 || StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        if (rightsService.can(userId, Permission.SUPER_ADMIN)) {
            return true;
        }
        return rightsService.roleOnThesaurus(userId, thesaurusId)
                .map(roleId -> roleId > 0 && roleId <= ProjectAccessPolicy.ROLE_ADMIN)
                .orElse(false);
    }

    /** Approuver / refuser / supprimer : droit board, pas l'auteur (sauf admin / super-admin). */
    public boolean canDecide(UserSession userSession, String thesaurusId, String authorEmail) {
        return canDecide(userSession, thesaurusId, authorEmail, null);
    }

    public boolean canDecide(UserSession userSession, String thesaurusId, String authorEmail, String authorName) {
        if (!isLoggedIn(userSession) || !canAccessBoard(userSession, thesaurusId)) {
            return false;
        }
        return !isSameAuthor(
                userSession.getCurrentUserEmail(),
                userSession.getCurrentUsername(),
                authorEmail,
                authorName)
                || canDecideOwn(userSession, thesaurusId);
    }

    public boolean canDecide(int userId, String thesaurusId, String userEmail, String authorEmail) {
        return canDecide(userId, thesaurusId, userEmail, null, authorEmail, null);
    }

    public boolean canDecide(
            int userId, String thesaurusId, String userEmail, String userName,
            String authorEmail, String authorName) {
        if (userId <= 0 || !canAccessBoard(userId, thesaurusId)) {
            return false;
        }
        return !isSameAuthor(userEmail, userName, authorEmail, authorName)
                || canDecideOwn(userId, thesaurusId);
    }

    public boolean isSameAuthor(UserSession userSession, String authorEmail, String authorName) {
        if (!isLoggedIn(userSession)) {
            return false;
        }
        return isSameAuthor(
                userSession.getCurrentUserEmail(),
                userSession.getCurrentUsername(),
                authorEmail,
                authorName);
    }

    static boolean isSameAuthor(String userEmail, String userName, String authorEmail, String authorName) {
        if (equalsIgnoreCase(userEmail, authorEmail)) {
            return true;
        }
        if (StringUtils.isNotBlank(userEmail) && StringUtils.isNotBlank(authorEmail)) {
            return false;
        }
        return equalsIgnoreCase(userName, authorName);
    }

    private static boolean equalsIgnoreCase(String first, String second) {
        return StringUtils.isNotBlank(first)
                && StringUtils.isNotBlank(second)
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private static boolean isLoggedIn(UserSession userSession) {
        return userSession != null && userSession.isLoggedIn();
    }
}
