package fr.cnrs.opentheso.v2.facet.api;

import fr.cnrs.opentheso.v2.concept.write.model.ConceptWriteFacet;
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
public class FacetSearchApiController {

    private final ConceptWriteSearchService conceptWriteSearchService;

    public FacetSearchApiController(ConceptWriteSearchService conceptWriteSearchService) {
        this.conceptWriteSearchService = conceptWriteSearchService;
    }

    @GetMapping(value = "/facets", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ConceptWriteFacet> list(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return List.of();
        }
        return conceptWriteSearchService.listFacets(StringUtils.firstNonBlank(lang, "fr"), thesaurusId);
    }
}
