package fr.cnrs.opentheso.ws.graphql;

import fr.cnrs.opentheso.models.concept.NodeFullConcept;
import fr.cnrs.opentheso.services.ResourceService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.shared.session.SessionAuthenticatedUserSource;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

@Controller
@AllArgsConstructor
public class GraphQLController {

    private final ResourceService resourceService;
    private final SearchService searchService;
    private final SessionAuthenticatedUserSource authenticatedUserSource;

    @QueryMapping
    public NodeFullConcept getNodeFullConcept(@Argument String idTheso,
                                              @Argument String idConcept,
                                              @Argument String idLang) {

        return resourceService.getFullConcept(idTheso, idConcept, idLang, 0, 10, hidePrivateGroups());
    }

    @QueryMapping
    public List<NodeFullConcept> searchConcept(
            @Argument String idTheso,
            @Argument String value,
            @Argument List<String> idGroups,
            @Argument String idLang) {

        List<String> listIds;
        if(CollectionUtils.isNotEmpty(idGroups)) {
            String[] stringArray;
            if(idGroups.size() == 1 && StringUtils.isBlank(idGroups.get(0))) {
                stringArray = null;
            } else {
                stringArray = idGroups.toArray(new String[0]);
            }
            listIds = searchService.searchAutoCompletionWSForWidget(value, idLang, stringArray, idTheso);
        } else {
            listIds = searchService.searchFullTextElasticId(value, idLang, idTheso, hidePrivateGroups());
        }
        if (listIds == null || listIds.isEmpty()) {
            return List.of();
        }
        boolean hidePrivateGroups = hidePrivateGroups();
        List<NodeFullConcept> nodeFullConcepts = new ArrayList<>();
        for (String idConcept : listIds) {
            if (StringUtils.isBlank(idConcept)) {
                continue;
            }
            NodeFullConcept concept = resourceService.getFullConcept(idTheso, idConcept, idLang, 0, 10, hidePrivateGroups);
            if (concept != null) {
                nodeFullConcepts.add(concept);
            }
        }
        return nodeFullConcepts;
    }

    private boolean hidePrivateGroups() {
        return !authenticatedUserSource.isLoggedIn();
    }
}