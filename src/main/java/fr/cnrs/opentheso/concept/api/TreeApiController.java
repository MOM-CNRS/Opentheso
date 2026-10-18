package fr.cnrs.opentheso.concept.api;

import fr.cnrs.opentheso.concept.api.dto.TreeStatusForestNode;
import fr.cnrs.opentheso.concept.model.ConceptPrefLabel;
import fr.cnrs.opentheso.concept.model.ConceptSubtreeMember;
import fr.cnrs.opentheso.concept.service.ConceptReadService;
import fr.cnrs.opentheso.concept.service.TreeStatusForestService;
import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TreeApiController {

    private final ConceptReadService conceptReadService;
    private final TreeStatusForestService treeStatusForestService;
    private final ThesaurusContext thesaurusContext;

    @GetMapping(value = "/subtree-size", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> subtreeSize(
            @RequestParam String id,
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) String thesaurusId
    ) {
        String resolvedThesaurusId = StringUtils.firstNonBlank(thesaurusId, thesaurusContext.resolveThesaurusId());
        int size = StringUtils.isAnyBlank(resolvedThesaurusId, id)
                ? 0
                : conceptReadService.countSubtreeConcepts(resolvedThesaurusId, id, nodeType);
        return Map.of("id", id, "size", size);
    }

    @GetMapping(value = "/subtree-members", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ConceptSubtreeMember> subtreeMembers(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false, defaultValue = "false") boolean all
    ) {
        String resolvedThesaurusId = StringUtils.firstNonBlank(thesaurusId, thesaurusContext.resolveThesaurusId());
        if (StringUtils.isBlank(resolvedThesaurusId)) {
            return List.of();
        }
        List<String> rootIds = StringUtils.isBlank(ids)
                ? List.of()
                : Arrays.stream(ids.split("[,\\s]+"))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .toList();
        return conceptReadService.listSubtreeMembers(
                resolvedThesaurusId,
                rootIds,
                StringUtils.firstNonBlank(lang, "fr"),
                all
        );
    }

    @GetMapping(value = "/concept-labels", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ConceptPrefLabel> conceptLabels(
            @RequestParam(required = false) String ids,
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        String resolvedThesaurusId = StringUtils.firstNonBlank(thesaurusId, thesaurusContext.resolveThesaurusId());
        if (StringUtils.isBlank(resolvedThesaurusId) || StringUtils.isBlank(ids)) {
            return List.of();
        }
        List<String> conceptIds = Arrays.stream(ids.split("[,\\s]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
        return conceptReadService.listPrefLabels(
                resolvedThesaurusId,
                conceptIds,
                StringUtils.firstNonBlank(lang, "fr")
        );
    }

    @GetMapping(value = "/tree-status-forest", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TreeStatusForestNode> statusForest(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String statuses
    ) {
        String resolvedThesaurusId = StringUtils.firstNonBlank(thesaurusId, thesaurusContext.resolveThesaurusId());
        if (StringUtils.isBlank(resolvedThesaurusId) || StringUtils.isBlank(statuses)) {
            return List.of();
        }
        List<String> selected = Arrays.stream(statuses.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
        return treeStatusForestService.loadForest(
                resolvedThesaurusId,
                StringUtils.firstNonBlank(lang, "fr"),
                selected
        );
    }
}
