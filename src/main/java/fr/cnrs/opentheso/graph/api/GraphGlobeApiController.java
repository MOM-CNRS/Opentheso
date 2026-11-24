package fr.cnrs.opentheso.graph.api;

import fr.cnrs.opentheso.graph.model.GraphNeighborhoodResponse;
import fr.cnrs.opentheso.graph.service.GraphGlobeConsultationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GraphGlobeApiController {

    private final GraphGlobeConsultationService graphGlobeConsultationService;

    public GraphGlobeApiController(GraphGlobeConsultationService graphGlobeConsultationService) {
        this.graphGlobeConsultationService = graphGlobeConsultationService;
    }

    @GetMapping(value = "/graph-neighborhood", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GraphNeighborhoodResponse> neighborhood(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String conceptId,
            @RequestParam(defaultValue = "0") int broaderOffset,
            @RequestParam(defaultValue = "0") int narrowerOffset,
            @RequestParam(defaultValue = "0") int relatedOffset
    ) {
        GraphNeighborhoodResponse body = graphGlobeConsultationService.loadNeighborhood(
                thesaurusId,
                StringUtils.firstNonBlank(lang, "fr"),
                StringUtils.trimToEmpty(conceptId),
                broaderOffset,
                narrowerOffset,
                relatedOffset
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
