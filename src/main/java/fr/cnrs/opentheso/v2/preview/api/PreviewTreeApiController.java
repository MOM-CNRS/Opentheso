package fr.cnrs.opentheso.v2.preview.api;

import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
@RequiredArgsConstructor
public class PreviewTreeApiController {

    private final ConceptReadService conceptReadService;
    private final ThesaurusContext thesaurusContext;

    @GetMapping(value = "/subtree-size", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> subtreeSize(
            @RequestParam String id,
            @RequestParam(required = false) String nodeType
    ) {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        int size = StringUtils.isAnyBlank(thesaurusId, id)
                ? 0
                : conceptReadService.countSubtreeConcepts(thesaurusId, id, nodeType);
        return Map.of("id", id, "size", size);
    }
}
