package fr.cnrs.opentheso.v2.concept.write.policy;

import fr.cnrs.opentheso.v2.rights.AuthTarget;
import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Façade d'écriture concept : délègue les seuils à {@link RightsService}
 * (rôle effectif sur le thésaurus courant) + règles UI (concept déprécié).
 */
@Component
@RequiredArgsConstructor
public class ConceptWritePolicy {

    private final RightsService rightsService;
    private final ThesaurusContext thesaurusContext;

    public boolean canMutateConcept(UserSession userSession) {
        return rightsService.can(userSession, Permission.MUTATE_CONCEPT, currentThesaurusTarget());
    }

    public boolean canMutateActiveConcept(UserSession userSession, boolean deprecated) {
        return canMutateConcept(userSession) && !deprecated;
    }

    public boolean canRenamePreferredLabel(UserSession userSession, boolean deprecated) {
        return !deprecated && rightsService.can(userSession, Permission.MUTATE_CONCEPT_STRUCTURE, currentThesaurusTarget());
    }

    public boolean canMutateHierarchicalRelations(UserSession userSession, boolean deprecated) {
        return !deprecated && rightsService.can(userSession, Permission.MUTATE_CONCEPT_STRUCTURE, currentThesaurusTarget());
    }

    public boolean canMutateLexicalContent(UserSession userSession, boolean deprecated) {
        return canMutateHierarchicalRelations(userSession, deprecated);
    }

    public boolean canMutateConceptStatus(UserSession userSession) {
        return rightsService.can(userSession, Permission.MUTATE_CONCEPT_STRUCTURE, currentThesaurusTarget());
    }

    public boolean canMutateCustomRelations(UserSession userSession, boolean deprecated) {
        return canMutateHierarchicalRelations(userSession, deprecated);
    }

    public boolean canMutateMedia(UserSession userSession, boolean deprecated) {
        return canMutateLexicalContent(userSession, deprecated);
    }

    public boolean canMutateAlignments(UserSession userSession, boolean deprecated) {
        return canMutateHierarchicalRelations(userSession, deprecated);
    }

    public boolean canMutateConceptAttributes(UserSession userSession, boolean deprecated) {
        return canMutateLexicalContent(userSession, deprecated);
    }

    public boolean canMutateIdentifiers(UserSession userSession) {
        return rightsService.can(userSession, Permission.MANAGE_THESAURUS, currentThesaurusTarget());
    }

    /** @deprecated préférer {@link #canMutateIdentifiers(UserSession)} */
    @Deprecated
    public boolean canMutateIdentifiers(UserSession userSession, boolean canManageThesaurus) {
        return userSession != null && userSession.isLoggedIn() && canManageThesaurus;
    }

    public boolean canTransferConcept(UserSession userSession) {
        return canMutateIdentifiers(userSession);
    }

    /** @deprecated préférer {@link #canTransferConcept(UserSession)} */
    @Deprecated
    public boolean canTransferConcept(UserSession userSession, boolean canManageThesaurus) {
        return canMutateIdentifiers(userSession, canManageThesaurus);
    }

    private AuthTarget currentThesaurusTarget() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        return StringUtils.isNotBlank(thesaurusId) ? AuthTarget.thesaurus(thesaurusId) : AuthTarget.none();
    }
}
