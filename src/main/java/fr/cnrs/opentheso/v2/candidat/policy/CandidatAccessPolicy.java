package fr.cnrs.opentheso.v2.candidat.policy;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Façade candidat : décisions via {@link RightsService}.
 * Centralise canCreate / canVote / canDiscuss / canProcess / canImport / canExport.
 */
@Component
@RequiredArgsConstructor
public class CandidatAccessPolicy {

    private final RightsService rightsService;

    public boolean canAccessModule(UserSession userSession) {
        return rightsService.can(userSession, Permission.ACCESS_CANDIDAT);
    }

    public boolean canAccessModule(UserSession userSession, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return canAccessModule(userSession);
        }
        return rightsService.can(userSession, Permission.ACCESS_CANDIDAT, AuthTarget.thesaurus(thesaurusId));
    }

    public boolean hasSelectedThesaurus(String thesaurusId) {
        return StringUtils.isNotBlank(thesaurusId);
    }

    public boolean canCreate(UserSession userSession, String thesaurusId) {
        return isLoggedIn(userSession) && canContribute(userSession, thesaurusId);
    }

    public boolean canVote(UserSession userSession, String thesaurusId) {
        return isLoggedIn(userSession) && canContribute(userSession, thesaurusId);
    }

    public boolean canDiscuss(UserSession userSession, String thesaurusId) {
        return isLoggedIn(userSession) && canContribute(userSession, thesaurusId);
    }

    public boolean canExport(UserSession userSession, String thesaurusId) {
        return canAccessModule(userSession, thesaurusId);
    }

    public boolean canImport(UserSession userSession, String thesaurusId) {
        return isThesaurusAdmin(userSession, thesaurusId);
    }

    public boolean canDelete(UserSession userSession, String thesaurusId) {
        return isThesaurusAdmin(userSession, thesaurusId);
    }

    public boolean canTransfer(UserSession userSession, String thesaurusId) {
        return isThesaurusAdmin(userSession, thesaurusId);
    }

    /**
     * Traitement (insérer / rejeter) : admin thésaurus ou super-admin, pas le proposant.
     */
    public boolean canProcess(UserSession userSession, String thesaurusId, Integer candidateCreatorId) {
        if (!isThesaurusAdmin(userSession, thesaurusId)) {
            return false;
        }
        Integer currentUserId = userSession != null ? userSession.getCurrentUserId() : null;
        if (currentUserId == null) {
            return false;
        }
        if (candidateCreatorId != null && candidateCreatorId > 0 && candidateCreatorId.equals(currentUserId)) {
            return false;
        }
        return true;
    }

    public boolean canReactivate(UserSession userSession, String thesaurusId) {
        return isThesaurusAdmin(userSession, thesaurusId);
    }

    private boolean canContribute(UserSession userSession, String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return rightsService.can(userSession, Permission.ACCESS_CANDIDAT)
                    || rightsService.can(userSession, Permission.CONTRIBUTE_ON_THESAURUS);
        }
        return rightsService.can(userSession, Permission.ACCESS_CANDIDAT, AuthTarget.thesaurus(thesaurusId))
                || rightsService.can(userSession, Permission.CONTRIBUTE_ON_THESAURUS, AuthTarget.thesaurus(thesaurusId));
    }

    private boolean isThesaurusAdmin(UserSession userSession, String thesaurusId) {
        if (!isLoggedIn(userSession)) {
            return false;
        }
        Integer userId = userSession.getCurrentUserId();
        if (rightsService.can(userId, Permission.SUPER_ADMIN)) {
            return true;
        }
        if (StringUtils.isBlank(thesaurusId)) {
            return false;
        }
        return rightsService.canOnThesaurus(userId, Permission.MANAGE_THESAURUS, thesaurusId);
    }

    private static boolean isLoggedIn(UserSession userSession) {
        return userSession != null && userSession.isLoggedIn();
    }
}
