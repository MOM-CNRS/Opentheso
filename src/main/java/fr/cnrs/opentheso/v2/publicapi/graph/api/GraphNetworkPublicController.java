package fr.cnrs.opentheso.v2.publicapi.graph.api;

import fr.cnrs.opentheso.v2.publicapi.graph.api.dto.D3jsGraphResponse;
import fr.cnrs.opentheso.v2.publicapi.graph.service.ConceptGraphNetworkService;
import fr.cnrs.opentheso.v2.publicapi.graph.service.ConceptGraphNetworkService.GraphRequestEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("v2PublicGraphNetworkController")
@RequestMapping("/openapi/v2/public/graph")
@RequiredArgsConstructor
@Tag(name = "Graphe réseau (public)", description = "Graphe multi-thésaurus/branches au format nœuds/relations D3js (v2, sans authentification)")
public class GraphNetworkPublicController {

    private final ConceptGraphNetworkService conceptGraphNetworkService;

    @GetMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Données de graphe pour un ou plusieurs thésaurus/branches",
            description = "idThesoConcept au format thesaurusId:conceptId (ou juste thesaurusId pour tout le thésaurus)")
    public D3jsGraphResponse getGraphData(
            @RequestParam String lang,
            @RequestParam("idThesoConcept") List<String> idThesoConcepts,
            @RequestParam(defaultValue = "false") boolean limit
    ) {
        List<GraphRequestEntry> entries = idThesoConcepts.stream()
                .map(this::parseEntry)
                .filter(java.util.Objects::nonNull)
                .toList();
        return conceptGraphNetworkService.buildGraph(entries, lang, limit);
    }

    private GraphRequestEntry parseEntry(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(":", 2);
        if (parts.length == 2) {
            return new GraphRequestEntry(parts[0], parts[1]);
        }
        return new GraphRequestEntry(parts[0], null);
    }
}
