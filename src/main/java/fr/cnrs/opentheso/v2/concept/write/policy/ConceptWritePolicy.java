package fr.cnrs.opentheso.v2.concept.write.policy;

import fr.cnrs.opentheso.v2.shared.ui.UserSession;

public final class ConceptWritePolicy {

    private ConceptWritePolicy() {
    }

    public static boolean canMutateConcept(UserSession userSession) {
        if (userSession == null || !userSession.isLoggedIn()) {
            return false;
        }
        return userSession.isContributor() || userSession.isManager() || userSession.isSuperAdmin();
    }

    public static boolean canMutateActiveConcept(UserSession userSession, boolean deprecated) {
        return canMutateConcept(userSession) && !deprecated;
    }

    public static boolean canRenamePreferredLabel(UserSession userSession, boolean deprecated) {
        if (userSession == null || !userSession.isLoggedIn() || deprecated) {
            return false;
        }
        return userSession.isManager() || userSession.isSuperAdmin();
    }

    public static boolean canMutateHierarchicalRelations(UserSession userSession, boolean deprecated) {
        if (userSession == null || !userSession.isLoggedIn() || deprecated) {
            return false;
        }
        return userSession.isManager() || userSession.isSuperAdmin();
    }

    public static boolean canMutateLexicalContent(UserSession userSession, boolean deprecated) {
        return canMutateHierarchicalRelations(userSession, deprecated);
    }

    public static boolean canMutateConceptStatus(UserSession userSession) {
        if (userSession == null || !userSession.isLoggedIn()) {
            return false;
        }
        return userSession.isManager() || userSession.isSuperAdmin();
    }

    public static boolean canMutateCustomRelations(UserSession userSession, boolean deprecated) {
        return canMutateHierarchicalRelations(userSession, deprecated);
    }

    public static boolean canMutateMedia(UserSession userSession, boolean deprecated) {
        return canMutateLexicalContent(userSession, deprecated);
    }

    public static boolean canMutateAlignments(UserSession userSession, boolean deprecated) {
        return canMutateHierarchicalRelations(userSession, deprecated);
    }

    public static boolean canMutateConceptAttributes(UserSession userSession, boolean deprecated) {
        return canMutateLexicalContent(userSession, deprecated);
    }

    public static boolean canMutateIdentifiers(UserSession userSession, boolean canManageThesaurus) {
        return userSession != null && userSession.isLoggedIn() && canManageThesaurus;
    }

    public static boolean canTransferConcept(UserSession userSession, boolean canManageThesaurus) {
        return canMutateIdentifiers(userSession, canManageThesaurus);
    }
}
