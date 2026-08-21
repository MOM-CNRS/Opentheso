package fr.cnrs.opentheso.v2.graph.api;

import fr.cnrs.opentheso.v2.graph.model.GraphGlobeResponse;
import fr.cnrs.opentheso.v2.graph.model.GraphNeighborhoodResponse;
import fr.cnrs.opentheso.v2.graph.service.GraphGlobeConsultationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping({"/v2/api", "/v2-preview/api"})
public class GraphGlobeApiController {

    private final GraphGlobeConsultationService graphGlobeConsultationService;

    public GraphGlobeApiController(GraphGlobeConsultationService graphGlobeConsultationService) {
        this.graphGlobeConsultationService = graphGlobeConsultationService;
    }

    @GetMapping(value = "/graph-globe", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GraphGlobeResponse> globe(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        if (StringUtils.isBlank(thesaurusId)) {
            return cached(new GraphGlobeResponse(List.of(), false));
        }
        return cached(graphGlobeConsultationService.loadGlobe(
                thesaurusId,
                StringUtils.firstNonBlank(lang, "fr")
        ));
    }

    @GetMapping(value = "/graph-neighborhood", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GraphNeighborhoodResponse> neighborhood(
            @RequestParam(required = false) String thesaurusId,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String conceptId
    ) {
        GraphNeighborhoodResponse body = graphGlobeConsultationService.loadNeighborhood(
                thesaurusId,
                StringUtils.firstNonBlank(lang, "fr"),
                StringUtils.trimToEmpty(conceptId)
        );
        return cached(body);
    }

    private static <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok()
                .eTag(Integer.toHexString(Objects.hashCode(body)))
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).cachePrivate())
                .header("Vary", "Cookie")
                .body(body);
    }
}
