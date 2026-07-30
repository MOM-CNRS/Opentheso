package fr.cnrs.opentheso.v2.graph.policy;

import fr.cnrs.opentheso.v2.rights.Permission;
import fr.cnrs.opentheso.v2.rights.RightsService;
import fr.cnrs.opentheso.v2.shared.ui.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Façade graphe : décisions via {@link RightsService}.
 */
@Component
@RequiredArgsConstructor
public class GraphAccessPolicy {

    private final RightsService rightsService;

    public boolean canAccessModule(UserSession userSession) {
        return rightsService.can(userSession, Permission.ACCESS_GRAPH);
    }
}
