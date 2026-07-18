package fr.cnrs.opentheso.v2.publicapi.concept.service;

import fr.cnrs.opentheso.repositories.ConceptRepository;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptTreeNodeResponse;
import fr.cnrs.opentheso.v2.concept.api.mapper.ConceptApiMapper;
import fr.cnrs.opentheso.v2.concept.service.ConceptBreadcrumbReadService;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.publicapi.concept.api.dto.ConceptSearchPathResponse;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConceptSearchPublicService {

    private final ConceptReadService conceptReadService;
    private final ConceptBreadcrumbReadService conceptBreadcrumbReadService;
    private final ConceptRepository conceptRepository;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    public List<ConceptTreeNodeResponse> search(String thesaurusId, String query, String lang, int limit) {
        String workLang = resolveLang(thesaurusId, lang);
        return conceptReadService.searchByLabel(thesaurusId, workLang, query, limit).stream()
                .map(ConceptApiMapper::toTreeNode)
                .toList();
    }

    public List<ConceptTreeNodeResponse> searchByNotation(String thesaurusId, String query, String lang, int limit) {
        String workLang = resolveLang(thesaurusId, lang);
        return conceptRepository.findAllByIdThesaurusAndNotationLike(thesaurusId, "%" + query + "%").stream()
                .limit(limit)
                .map(concept -> {
                    String label = conceptReadService.loadSummary(thesaurusId, concept.getIdConcept(), workLang)
                            .map(summary -> summary.preferredLabel())
                            .orElse("(" + concept.getIdConcept() + ")");
                    return new ConceptTreeNodeResponse(
                            concept.getIdConcept(), label, concept.getNotation(), concept.getConceptType(), false);
                })
                .toList();
    }

    public List<ConceptTreeNodeResponse> autocomplete(String thesaurusId, String input, String lang, int limit) {
        return search(thesaurusId, input, lang, limit);
    }

    public List<ConceptTreeNodeResponse> rootConceptGroups(String thesaurusId, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        return conceptReadService.loadRootNodes(thesaurusId, workLang).stream()
                .map(ConceptApiMapper::toTreeNode)
                .toList();
    }

    public ConceptSearchPathResponse fullPathOfConcept(String thesaurusId, String conceptId, String lang) {
        String workLang = resolveLang(thesaurusId, lang);
        String label = conceptReadService.loadSummary(thesaurusId, conceptId, workLang)
                .map(summary -> summary.preferredLabel())
                .orElse(conceptId);
        return new ConceptSearchPathResponse(conceptId, label, loadAllPaths(thesaurusId, conceptId, workLang));
    }

    public List<ConceptSearchPathResponse> searchWithFullPath(String thesaurusId, String query, String lang, int limit) {
        String workLang = resolveLang(thesaurusId, lang);
        return conceptReadService.searchByLabel(thesaurusId, workLang, query, limit).stream()
                .map(node -> new ConceptSearchPathResponse(
                        node.getNodeId(),
                        node.getLabel(),
                        loadAllPaths(thesaurusId, node.getNodeId(), workLang)
                ))
                .toList();
    }

    private List<List<fr.cnrs.opentheso.v2.concept.api.dto.ConceptBreadcrumbResponse>> loadAllPaths(
            String thesaurusId, String conceptId, String lang) {
        return conceptBreadcrumbReadService.loadBreadcrumbPaths(thesaurusId, conceptId, lang).stream()
                .map(path -> path.stream().map(ConceptApiMapper::toBreadcrumb).toList())
                .toList();
    }

    private String resolveLang(String thesaurusId, String lang) {
        return StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }
}
