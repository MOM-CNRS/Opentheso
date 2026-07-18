package fr.cnrs.opentheso.v2.publicapi.graph.api;

import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsTreeNodeResponse;
import fr.cnrs.opentheso.v2.publicapi.graph.service.ConceptGraphTreeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("v2PublicConceptGraphController")
@RequestMapping("/openapi/v2/public/thesauri/{thesaurusId}")
@RequiredArgsConstructor
@Tag(name = "Graphe D3js (public)", description = "Arbre de concepts au format D3js (v2, sans authentification)")
public class ConceptGraphPublicController {

    private final ConceptGraphTreeService conceptGraphTreeService;

    @GetMapping(value = "/graph", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Arbre D3js de tout le thésaurus")
    public D3jsTreeNodeResponse thesaurusGraph(
            @PathVariable String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "false") boolean limit
    ) {
        return conceptGraphTreeService.buildThesaurusTree(thesaurusId, lang, limit);
    }

    @GetMapping(value = "/concepts/{conceptId}/graph", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Arbre D3js d'une branche partant d'un concept")
    public D3jsTreeNodeResponse conceptGraph(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang,
            @RequestParam(defaultValue = "false") boolean limit
    ) {
        return conceptGraphTreeService.buildConceptTree(thesaurusId, conceptId, lang, limit);
    }
}
