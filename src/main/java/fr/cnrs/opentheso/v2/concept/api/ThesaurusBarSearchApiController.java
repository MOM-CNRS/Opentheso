package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.search.model.ConceptSearchMode;
import fr.cnrs.opentheso.v2.concept.search.model.ThesaurusBarSearchResponse;
import fr.cnrs.opentheso.v2.concept.search.service.ThesaurusBarSearchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class ThesaurusBarSearchApiController {

    private final ThesaurusBarSearchService thesaurusBarSearchService;

    public ThesaurusBarSearchApiController(ThesaurusBarSearchService thesaurusBarSearchService) {
        this.thesaurusBarSearchService = thesaurusBarSearchService;
    }

    @GetMapping(value = "/thesaurus-search/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ThesaurusBarSearchResponse suggest(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String mode
    ) {
        return thesaurusBarSearchService.suggest(thesaurusId, lang, q, parseMode(mode));
    }

    @GetMapping(value = "/thesaurus-search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ThesaurusBarSearchResponse search(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "24") int limit
    ) {
        return thesaurusBarSearchService.search(thesaurusId, lang, q, parseMode(mode), offset, limit);
    }

    @PostMapping(value = "/thesaurus-search/select")
    public void select(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String label
    ) {
        thesaurusBarSearchService.logSuggestionSelected(thesaurusId, lang, q, label);
    }

    @GetMapping(value = "/thesaurus-search/programmed", produces = MediaType.APPLICATION_JSON_VALUE)
    public ThesaurusBarSearchResponse programmed(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "24") int limit
    ) {
        return thesaurusBarSearchService.programmed(thesaurusId, lang, kind, offset, limit);
    }

    private static ConceptSearchMode parseMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return ConceptSearchMode.FULL_TEXT;
        }
        try {
            return ConceptSearchMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ConceptSearchMode.FULL_TEXT;
        }
    }
}
