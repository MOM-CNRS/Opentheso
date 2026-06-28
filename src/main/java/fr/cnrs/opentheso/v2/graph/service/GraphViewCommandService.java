package fr.cnrs.opentheso.v2.graph.service;

import fr.cnrs.opentheso.entites.GraphView;
import fr.cnrs.opentheso.entites.GraphViewExportedConceptBranch;
import fr.cnrs.opentheso.repositories.GraphViewExportedConceptBranchRepository;
import fr.cnrs.opentheso.repositories.GraphViewRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GraphViewCommandService {

    private final GraphViewRepository graphViewRepository;
    private final GraphViewExportedConceptBranchRepository exportRepository;

    @Transactional
    public int createView(String name, String description, int userId) {
        var saved = graphViewRepository.save(GraphView.builder()
                .name(name)
                .description(description)
                .idUser(userId)
                .build());
        return saved.getId();
    }

    @Transactional
    public void updateView(int viewId, String name, String description) {
        var graphView = graphViewRepository.getById(viewId);
        graphView.setName(name);
        graphView.setDescription(description);
        graphViewRepository.save(graphView);
    }

    @Transactional
    public void deleteView(int viewId) {
        graphViewRepository.deleteById(viewId);
        exportRepository.deleteAllByGraphViewId(viewId);
    }

    public void deleteView(String viewId) {
        deleteView(Integer.parseInt(viewId));
    }

    @Transactional
    public boolean addExportEntry(int viewId, String thesaurusId, String conceptId) {
        if (existsExportEntry(viewId, thesaurusId, conceptId)) {
            return false;
        }
        exportRepository.save(GraphViewExportedConceptBranch.builder()
                .graphViewId(viewId)
                .topConceptThesaurusId(thesaurusId)
                .topConceptId(conceptId)
                .build());
        return true;
    }

    public boolean existsExportEntry(int viewId, String thesaurusId, String conceptId) {
        if (StringUtils.isBlank(conceptId)) {
            return exportRepository
                    .findByGraphViewIdAndTopConceptIdNullAndTopConceptThesaurusId(viewId, thesaurusId)
                    .isPresent();
        }
        return exportRepository
                .findByGraphViewIdAndTopConceptIdAndTopConceptThesaurusId(viewId, conceptId, thesaurusId)
                .isPresent();
    }

    @Transactional
    public void removeExportEntry(int viewId, String thesaurusId, String conceptId) {
        if (StringUtils.isBlank(conceptId)) {
            exportRepository.deleteAllByGraphViewIdAndTopConceptThesaurusId(viewId, thesaurusId);
        } else {
            exportRepository.deleteAllByGraphViewIdAndTopConceptIdAndTopConceptThesaurusId(
                    viewId, conceptId, thesaurusId);
        }
    }
}
