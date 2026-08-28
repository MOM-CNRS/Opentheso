package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.write.service.ConceptWriteSearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class ConceptSearchApiController {

    private final ConceptWriteSearchService conceptWriteSearchService;

    public ConceptSearchApiController(ConceptWriteSearchService conceptWriteSearchService) {
        this.conceptWriteSearchService = conceptWriteSearchService;
    }

    @GetMapping(value = "/concepts/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ConceptSearchHit> search(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String excludeId,
            @RequestParam(required = false, defaultValue = "false") boolean customOnly
    ) {
        if (StringUtils.isAnyBlank(thesaurusId, q)) {
            return List.of();
        }
        String workLang = StringUtils.firstNonBlank(lang, "fr");
        String exclude = StringUtils.trimToEmpty(excludeId);
        if (customOnly) {
            return conceptWriteSearchService.autocompleteCustomRelationTarget(q.trim(), workLang, thesaurusId)
                    .stream()
                    .filter(hit -> hit != null && StringUtils.isNotBlank(hit.id()))
                    .filter(hit -> exclude.isEmpty() || !hit.id().equalsIgnoreCase(exclude))
                    .map(hit -> new ConceptSearchHit(hit.id(), hit.label(), hit.type()))
                    .toList();
        }
        return conceptWriteSearchService.autocompleteRelationTarget(q.trim(), workLang, thesaurusId, true)
                .stream()
                .filter(suggestion -> suggestion != null && StringUtils.isNotBlank(suggestion.conceptId()))
                .filter(suggestion -> exclude.isEmpty() || !suggestion.conceptId().equalsIgnoreCase(exclude))
                .map(suggestion -> new ConceptSearchHit(suggestion.conceptId(), suggestion.displayLabel()))
                .toList();
    }
}
