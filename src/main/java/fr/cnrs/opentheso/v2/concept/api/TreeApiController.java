package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.api.dto.TreeStatusForestNode;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.concept.service.TreeStatusForestService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
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
@RequestMapping({"/v2/api", "/v2-preview/api"})
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
