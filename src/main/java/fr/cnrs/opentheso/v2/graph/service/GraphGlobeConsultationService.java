package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.v2.concept.policy.ConceptStatusPolicy;
import fr.cnrs.opentheso.v2.graph.model.GraphGlobeNode;
import fr.cnrs.opentheso.v2.graph.model.GraphGlobeResponse;
import fr.cnrs.opentheso.v2.graph.model.GraphNeighbor;
import fr.cnrs.opentheso.v2.graph.model.GraphNeighborhoodResponse;
import fr.cnrs.opentheso.v2.shared.repository.GraphGlobeQueryRepository;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GraphGlobeConsultationService {

    static final int MAX_NODES = 15_000;
    static final int MAX_NEIGHBORS = 120;
    static final int MAX_NEIGHBORS_PER_ROLE = 40;

    private final GraphGlobeQueryRepository graphGlobeQueryRepository;
    private final AuthenticatedUserSource authenticatedUserSource;

    public GraphGlobeConsultationService(
            GraphGlobeQueryRepository graphGlobeQueryRepository,
            AuthenticatedUserSource authenticatedUserSource
    ) {
        this.graphGlobeQueryRepository = graphGlobeQueryRepository;
        this.authenticatedUserSource = authenticatedUserSource;
    }

    @Transactional(readOnly = true)
    public GraphGlobeResponse loadGlobe(String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return new GraphGlobeResponse(List.of(), false);
        }
        boolean includeCandidates = authenticatedUserSource.isLoggedIn();
        List<Object[]> raw = graphGlobeQueryRepository.findGlobeConcepts(
                thesaurusId, lang, includeCandidates, MAX_NODES + 1);
        boolean truncated = raw.size() > MAX_NODES;
        if (truncated) {
            raw = new ArrayList<>(raw.subList(0, MAX_NODES));
        }
        List<GraphGlobeNode> nodes = new ArrayList<>(raw.size());
        for (Object[] row : raw) {
            String id = str(row, 0);
            String label = str(row, 1);
            if (label.isEmpty()) {
                label = id;
            }
            nodes.add(new GraphGlobeNode(id, label, uiStatus(str(row, 2))));
        }
        return new GraphGlobeResponse(List.copyOf(nodes), truncated);
    }

    @Transactional(readOnly = true)
    public GraphNeighborhoodResponse loadNeighborhood(String thesaurusId, String lang, String conceptId) {
        if (StringUtils.isAnyBlank(thesaurusId, lang, conceptId)) {
            return emptyNeighborhood(conceptId);
        }
        List<Object[]> raw = graphGlobeQueryRepository.findNeighborhood(
                thesaurusId, conceptId, lang, MAX_NEIGHBORS);
        List<GraphNeighbor> broader = new ArrayList<>();
        List<GraphNeighbor> narrower = new ArrayList<>();
        List<GraphNeighbor> related = new ArrayList<>();
        for (Object[] row : raw) {
            String id = str(row, 0);
            if (id.isEmpty()) {
                continue;
            }
            String label = str(row, 1);
            if (label.isEmpty()) {
                label = id;
            }
            String role = str(row, 2).toUpperCase();
            if (role.startsWith("BT") && broader.size() < MAX_NEIGHBORS_PER_ROLE) {
                broader.add(new GraphNeighbor(id, label, "TG"));
            } else if (role.startsWith("NT") && narrower.size() < MAX_NEIGHBORS_PER_ROLE) {
                narrower.add(new GraphNeighbor(id, label, "TS"));
            } else if (role.startsWith("RT") && related.size() < MAX_NEIGHBORS_PER_ROLE) {
                related.add(new GraphNeighbor(id, label, "TA"));
            }
        }
        return new GraphNeighborhoodResponse(
                conceptId,
                List.copyOf(broader),
                List.copyOf(narrower),
                List.copyOf(related)
        );
    }

    private static GraphNeighborhoodResponse emptyNeighborhood(String conceptId) {
        return new GraphNeighborhoodResponse(
                StringUtils.defaultString(conceptId),
                List.of(),
                List.of(),
                List.of()
        );
    }

    static String uiStatus(String dbStatus) {
        if ("CA".equalsIgnoreCase(StringUtils.trimToEmpty(dbStatus))) {
            return "candidat";
        }
        if (ConceptStatusPolicy.isDeprecated(dbStatus)) {
            return "deprecie";
        }
        return "valide";
    }

    private static String str(Object[] row, int index) {
        if (row == null || index >= row.length || row[index] == null) {
            return "";
        }
        return String.valueOf(row[index]).trim();
    }
}
