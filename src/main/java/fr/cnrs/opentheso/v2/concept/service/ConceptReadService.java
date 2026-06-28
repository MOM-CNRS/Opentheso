package fr.cnrs.opentheso.v2.concept.service;

import fr.cnrs.opentheso.v2.concept.model.*;
import fr.cnrs.opentheso.v2.shared.repository.ConceptQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConceptReadService {

    private final ConceptQueryRepository repo;

    private static final int SEARCH_LIMIT = 50;

    public List<ConceptGroup> loadGroups(String thesaurusId, String lang) {
        return repo.findConceptGroups(thesaurusId, lang)
                .stream().map(ConceptMapper::toGroup).toList();
    }

    public List<ConceptTreeNode> loadTopConcepts(String groupId, String thesaurusId, String lang) {
        return repo.findTopConceptsOfGroup(groupId, thesaurusId, lang)
                .stream().map(ConceptMapper::toTreeNode).toList();
    }

    public List<ConceptTreeNode> loadTopConceptsWithoutGroup(String thesaurusId, String lang) {
        return repo.findTopConceptsWithoutGroup(thesaurusId, lang)
                .stream().map(ConceptMapper::toTreeNode).toList();
    }

    public List<ConceptTreeNode> loadChildren(String parentId, String thesaurusId, String lang) {
        return repo.findChildConcepts(parentId, thesaurusId, lang)
                .stream().map(ConceptMapper::toTreeNode).toList();
    }

    public Optional<ConceptDetail> loadDetail(String conceptId, String thesaurusId, String lang) {
        return repo.findConceptHeader(conceptId, thesaurusId, lang)
                .map(header -> ConceptMapper.toDetail(
                        header,
                        repo.findConceptLabels(conceptId, thesaurusId),
                        repo.findConceptRelations(conceptId, thesaurusId, lang),
                        repo.findBreadcrumb(conceptId, thesaurusId, lang),
                        repo.findConceptNotes(conceptId, thesaurusId, lang),
                        repo.findConceptAlignments(conceptId, thesaurusId)
                ));
    }

    public List<ConceptTreeNode> search(String thesaurusId, String lang, String query) {
        if (query == null || query.isBlank()) return List.of();
        return repo.searchByLabel(thesaurusId, lang, query, SEARCH_LIMIT)
                .stream().map(ConceptMapper::toTreeNode).toList();
    }

    public boolean thesaurusHasGroups(String thesaurusId) {
        return repo.hasGroups(thesaurusId);
    }
}
