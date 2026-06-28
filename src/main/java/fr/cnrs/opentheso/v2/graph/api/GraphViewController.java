package fr.cnrs.opentheso.v2.graph.api;

import fr.cnrs.opentheso.v2.graph.api.dto.AddGraphExportRequest;
import fr.cnrs.opentheso.v2.graph.api.dto.CreateGraphViewRequest;
import fr.cnrs.opentheso.v2.graph.api.dto.GraphViewResponse;
import fr.cnrs.opentheso.v2.graph.api.dto.GraphVisualizationUrlResponse;
import fr.cnrs.opentheso.v2.graph.api.dto.UpdateGraphViewRequest;
import fr.cnrs.opentheso.v2.graph.api.mapper.GraphApiMapper;
import fr.cnrs.opentheso.v2.graph.exception.InvalidGraphDataException;
import fr.cnrs.opentheso.v2.graph.service.GraphNeo4jExportService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewCommandService;
import fr.cnrs.opentheso.v2.graph.service.GraphViewReadService;
import fr.cnrs.opentheso.v2.graph.service.GraphVisualizationUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/openapi/v2/graph/views")
@RequiredArgsConstructor
@Tag(name = "Graphe", description = "Gestion des vues graphe sauvegardées (v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class GraphViewController {

    private final GraphAuthSupport graphAuthSupport;
    private final GraphViewReadService graphViewReadService;
    private final GraphViewCommandService graphViewCommandService;
    private final GraphVisualizationUrlService graphVisualizationUrlService;
    private final GraphNeo4jExportService graphNeo4jExportService;

    @Value("${opentheso.url:http://localhost:8099}")
    private String openthesoBaseUrl;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les vues graphe de l'utilisateur")
    public List<GraphViewResponse> listViews(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        return GraphApiMapper.toResponses(graphViewReadService.loadViewsForUser(userId));
    }

    @GetMapping(value = "/{viewId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lire une vue graphe")
    public GraphViewResponse getView(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int viewId
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        return GraphApiMapper.toResponse(graphViewReadService.requireViewForUser(viewId, userId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer une vue graphe")
    public GraphViewResponse createView(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @Valid @RequestBody CreateGraphViewRequest request
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        int viewId = graphViewCommandService.createView(request.name(), request.description(), userId);
        return GraphApiMapper.toResponse(graphViewReadService.requireViewForUser(viewId, userId));
    }

    @PutMapping(value = "/{viewId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mettre à jour une vue graphe")
    public GraphViewResponse updateView(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int viewId,
            @Valid @RequestBody UpdateGraphViewRequest request
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        graphViewReadService.requireViewForUser(viewId, userId);
        graphViewCommandService.updateView(viewId, request.name(), request.description());
        return GraphApiMapper.toResponse(graphViewReadService.requireViewForUser(viewId, userId));
    }

    @DeleteMapping("/{viewId}")
    @Operation(summary = "Supprimer une vue graphe")
    public ResponseEntity<Void> deleteView(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int viewId
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        graphViewReadService.requireViewForUser(viewId, userId);
        graphViewCommandService.deleteView(viewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{viewId}/exports", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ajouter une entrée d'export à une vue")
    public GraphViewResponse addExport(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int viewId,
            @Valid @RequestBody AddGraphExportRequest request
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        graphViewReadService.requireViewForUser(viewId, userId);
        if (!graphViewCommandService.addExportEntry(viewId, request.thesaurusId(), request.conceptId())) {
            throw new InvalidGraphDataException("Cette combinaison d'export existe déjà");
        }
        return GraphApiMapper.toResponse(graphViewReadService.requireViewForUser(viewId, userId));
    }

    @DeleteMapping("/{viewId}/exports")
    @Operation(summary = "Retirer une entrée d'export d'une vue")
    public GraphViewResponse removeExport(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int viewId,
            @RequestParam String thesaurusId,
            @RequestParam(required = false) String conceptId
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        graphViewReadService.requireViewForUser(viewId, userId);
        graphViewCommandService.removeExportEntry(viewId, thesaurusId, conceptId);
        return GraphApiMapper.toResponse(graphViewReadService.requireViewForUser(viewId, userId));
    }

    @GetMapping(value = "/{viewId}/visualization-url", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Obtenir l'URL de visualisation force-directed")
    public GraphVisualizationUrlResponse visualizationUrl(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int viewId,
            @RequestParam(required = false) String lang
    ) throws URISyntaxException {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        graphViewReadService.requireViewForUser(viewId, userId);
        String url = graphVisualizationUrlService.buildVisualizationUrl(
                String.valueOf(viewId),
                openthesoBaseUrl,
                lang
        );
        return new GraphVisualizationUrlResponse(url);
    }

    @PostMapping("/{viewId}/neo4j-export")
    @Operation(summary = "Exporter la vue vers Neo4j")
    public ResponseEntity<Void> exportToNeo4j(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable int viewId
    ) {
        int userId = graphAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        graphAuthSupport.requireAuthenticated(userId);
        graphViewReadService.requireViewForUser(viewId, userId);
        graphNeo4jExportService.exportView(String.valueOf(viewId), openthesoBaseUrl);
        return ResponseEntity.accepted().build();
    }
}
