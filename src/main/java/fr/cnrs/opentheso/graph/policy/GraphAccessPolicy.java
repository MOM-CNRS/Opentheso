package fr.cnrs.opentheso.graph.policy;

import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import fr.cnrs.opentheso.shared.ui.UserSession;
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
