package fr.cnrs.opentheso.graph.service;

import fr.cnrs.opentheso.graph.mapper.GraphViewMapper;
import fr.cnrs.opentheso.graph.model.GraphViewSummary;
import fr.cnrs.opentheso.shared.repository.GraphViewQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GraphViewReadService {

    private final GraphViewQueryRepository graphViewQueryRepository;

    public List<GraphViewSummary> loadViewsForUser(int userId) {
        return GraphViewMapper.toSummaries(graphViewQueryRepository.findViewsByUserId(userId));
    }

    public GraphViewSummary loadView(int viewId) {
        return graphViewQueryRepository.findViewById(viewId)
                .map(GraphViewMapper::toSummary)
                .orElse(null);
    }

    public GraphViewSummary loadView(String viewId) {
        return loadView(Integer.parseInt(viewId));
    }

    public List<GraphViewSummary> reloadViewsForUser(int userId) {
        return new ArrayList<>(loadViewsForUser(userId));
    }

    public GraphViewSummary requireViewForUser(int viewId, int userId) {
        if (!graphViewQueryRepository.isViewOwnedByUser(viewId, userId)) {
            throw new fr.cnrs.opentheso.graph.exception.GraphViewNotFoundException(viewId);
        }
        return loadView(viewId);
    }
}
