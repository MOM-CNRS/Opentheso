package fr.cnrs.opentheso.graph.service;

import fr.cnrs.opentheso.entites.Preferences;
import fr.cnrs.opentheso.repositories.PreferencesRepository;
import fr.cnrs.opentheso.concept.policy.ConceptStatusPolicy;
import fr.cnrs.opentheso.concept.policy.ThesaurusConsultationAccessPolicy;
import fr.cnrs.opentheso.graph.model.GraphGlobeNode;
import fr.cnrs.opentheso.graph.model.GraphGlobeResponse;
import fr.cnrs.opentheso.graph.model.GraphNeighbor;
import fr.cnrs.opentheso.graph.model.GraphNeighborGroup;
import fr.cnrs.opentheso.graph.model.GraphNeighborhoodResponse;
import fr.cnrs.opentheso.shared.repository.GraphGlobeQueryRepository;
import fr.cnrs.opentheso.shared.repository.GraphGlobeQueryRepository.NeighborPage;
import fr.cnrs.opentheso.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.rights.Permission;
import fr.cnrs.opentheso.rights.RightsService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GraphGlobeConsultationService {

    static final int MAX_NODES = 15_000;
    static final int NEIGHBORS_PER_ROLE = 30;

    private final GraphGlobeQueryRepository graphGlobeQueryRepository;
    private final AuthenticatedUserSource authenticatedUserSource;
    private final PreferencesRepository preferencesRepository;
    private final ThesaurusConsultationAccessPolicy consultationAccessPolicy;
    private final RightsService rightsService;

    public GraphGlobeConsultationService(
            GraphGlobeQueryRepository graphGlobeQueryRepository,
            AuthenticatedUserSource authenticatedUserSource,
            PreferencesRepository preferencesRepository,
            ThesaurusConsultationAccessPolicy consultationAccessPolicy,
            RightsService rightsService
    ) {
        this.graphGlobeQueryRepository = graphGlobeQueryRepository;
        this.authenticatedUserSource = authenticatedUserSource;
        this.preferencesRepository = preferencesRepository;
        this.consultationAccessPolicy = consultationAccessPolicy;
        this.rightsService = rightsService;
    }

    @Transactional(readOnly = true)
    public GraphGlobeResponse loadGlobe(String thesaurusId, String lang) {
        if (StringUtils.isAnyBlank(thesaurusId, lang)) {
            return new GraphGlobeResponse(List.of(), false);
        }
        boolean includeCandidates = shouldIncludeCandidates(thesaurusId);
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

    /**
     * Concept central et ses voisins directs, avec les règles de visibilité de la fiche
     * (thésaurus privé, candidats, groupes privés).
     */
    @Transactional(readOnly = true)
    public GraphNeighborhoodResponse loadNeighborhood(
            String thesaurusId,
            String lang,
            String conceptId,
            int broaderOffset,
            int narrowerOffset,
            int relatedOffset
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, lang, conceptId) || !canRead(thesaurusId)) {
            return GraphNeighborhoodResponse.notFound(conceptId, NEIGHBORS_PER_ROLE);
        }
        boolean includeCandidates = shouldIncludeCandidates(thesaurusId);
        Optional<Object[]> center = graphGlobeQueryRepository.findCenter(
                thesaurusId, conceptId, lang, includeCandidates);
        if (center.isEmpty()) {
            return GraphNeighborhoodResponse.notFound(conceptId, NEIGHBORS_PER_ROLE);
        }
        NeighborPage page = new NeighborPage(
                broaderOffset, narrowerOffset, relatedOffset, NEIGHBORS_PER_ROLE);
        List<Object[]> rows = graphGlobeQueryRepository.findNeighborhood(
                thesaurusId, conceptId, lang, includeCandidates,
                authenticatedUserSource.isLoggedIn(), page);
        return new GraphNeighborhoodResponse(
                conceptId,
                labelOr(center.get(), conceptId),
                uiStatus(str(center.get(), 2)),
                true,
                NEIGHBORS_PER_ROLE,
                group(rows, "BT", "TG", page.broaderOffset()),
                group(rows, "NT", "TS", page.narrowerOffset()),
                group(rows, "RT", "TA", page.relatedOffset())
        );
    }

    private static GraphNeighborGroup group(List<Object[]> rows, String family, String role, int offset) {
        List<GraphNeighbor> items = new ArrayList<>();
        int total = 0;
        for (Object[] row : rows) {
            String id = str(row, 0);
            if (id.isEmpty() || !family.equals(str(row, 4))) {
                continue;
            }
            items.add(new GraphNeighbor(id, labelOr(row, id), role, str(row, 2), uiStatus(str(row, 3))));
            if (row[5] instanceof Number number) {
                total = number.intValue();
            }
        }
        return new GraphNeighborGroup(List.copyOf(items), total, offset);
    }

    private static String labelOr(Object[] row, String fallback) {
        String label = str(row, 1);
        return label.isEmpty() ? fallback : label;
    }

    private boolean canRead(String thesaurusId) {
        if (consultationAccessPolicy.canConsult(thesaurusId, false)) {
            return true;
        }
        Integer userId = authenticatedUserSource.getUserId().orElse(null);
        if (userId == null) {
            return false;
        }
        return rightsService.can(userId, Permission.SUPER_ADMIN)
                || rightsService.roleOnThesaurus(userId, thesaurusId).isPresent();
    }

    private boolean shouldIncludeCandidates(String thesaurusId) {
        if (authenticatedUserSource.isLoggedIn()) {
            return true;
        }
        return preferencesRepository.findByIdThesaurus(thesaurusId)
                .map(Preferences::isShowCandidatesToGuests)
                .orElse(false);
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
