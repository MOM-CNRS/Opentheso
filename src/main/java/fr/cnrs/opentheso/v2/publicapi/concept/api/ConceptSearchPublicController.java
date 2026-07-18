package fr.cnrs.opentheso.v2.publicapi.concept.api;

import fr.cnrs.opentheso.v2.concept.api.dto.ConceptTreeNodeResponse;
import fr.cnrs.opentheso.v2.publicapi.concept.api.dto.ConceptSearchPathResponse;
import fr.cnrs.opentheso.v2.publicapi.concept.service.ConceptSearchPublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("v2PublicConceptSearchController")
@RequestMapping("/openapi/v2/public/thesauri/{thesaurusId}/concepts")
@RequiredArgsConstructor
@Tag(name = "Recherche de concepts (public)", description = "Recherche et autocomplétion publiques de concepts (v2, sans authentification)")
public class ConceptSearchPublicController {

    private final ConceptSearchPublicService conceptSearchPublicService;

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recherche de concepts par libellé")
    public List<ConceptTreeNodeResponse> search(
            @PathVariable String thesaurusId,
            @RequestParam String q,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return conceptSearchPublicService.search(thesaurusId, q, lang, limit);
    }

    @GetMapping(value = "/search/notation", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recherche de concepts par notation")
    public List<ConceptTreeNodeResponse> searchByNotation(
            @PathVariable String thesaurusId,
            @RequestParam String q,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return conceptSearchPublicService.searchByNotation(thesaurusId, q, lang, limit);
    }

    @GetMapping(value = "/search/fullpath", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recherche de concepts avec fil d'Ariane complet")
    public List<ConceptSearchPathResponse> searchWithFullPath(
            @PathVariable String thesaurusId,
            @RequestParam String q,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return conceptSearchPublicService.searchWithFullPath(thesaurusId, q, lang, limit);
    }

    @GetMapping(value = "/autocomplete/{input}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Autocomplétion de concepts par préfixe")
    public List<ConceptTreeNodeResponse> autocomplete(
            @PathVariable String thesaurusId,
            @PathVariable String input,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "25") int limit
    ) {
        return conceptSearchPublicService.autocomplete(thesaurusId, input, lang, limit);
    }

    @GetMapping(value = "/autocomplete-groups", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Groupes racine pour initialiser un widget d'autocomplétion")
    public List<ConceptTreeNodeResponse> autocompleteGroups(
            @PathVariable String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        return conceptSearchPublicService.rootConceptGroups(thesaurusId, lang);
    }

    @GetMapping(value = "/{conceptId}/fullpath", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fil d'Ariane complet d'un concept jusqu'à la racine")
    public ConceptSearchPathResponse fullPathOfConcept(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        return conceptSearchPublicService.fullPathOfConcept(thesaurusId, conceptId, lang);
    }
}
