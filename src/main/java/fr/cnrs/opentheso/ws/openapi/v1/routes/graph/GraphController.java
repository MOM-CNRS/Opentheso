package fr.cnrs.opentheso.ws.openapi.v1.routes.graph;


import fr.cnrs.opentheso.ws.openapi.helper.d3jsgraph.GraphD3jsHelper;
import fr.cnrs.opentheso.ws.openapi.helper.d3jsgraph.IdValuePair;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static fr.cnrs.opentheso.ws.openapi.helper.CustomMediaType.APPLICATION_JSON_UTF_8;


@Slf4j
@RestController
@RequestMapping("/openapi/v1/graph/getData")
@CrossOrigin(methods = { RequestMethod.GET })
@Tag(name = "Graphe", description = "Actions pour récupérer les données au format graphe")
public class GraphController {

    @Autowired
    private GraphD3jsHelper graphD3jsHelper;


    @GetMapping(produces = APPLICATION_JSON_UTF_8)
    @Operation(summary = "Permet d'obtenir les données d'un thésaurus ou branche pour graphe D3js",
            tags = {"Graph"},
            description = "http://localhost:8080/opentheso2/openapi/v1/graph/getData?lang=fr&idThesoConcept=th3,4&idThesoConcept=th3",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fichier contenent le résultat de la recherche", content = {
                            @Content(mediaType = APPLICATION_JSON_UTF_8),
                    }),
                    @ApiResponse(responseCode = "400", description = "Erreur dans la synthaxe de la requête"),
                    @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
            })
    public ResponseEntity<Object>  getGraph(
            @Parameter(name = "lang", example = "fr", in = ParameterIn.QUERY, schema = @Schema(type = "string"), required = true, description = "Langue principale pour récupérer les concepts") @RequestParam("lang") String lang,
            @Parameter(name = "idThesoConcept", in = ParameterIn.QUERY, schema = @Schema(type = "string"), required = true, example = "th3:4" ,
                    description = "ID du thesaurus à récupérer et du concept (pour une branche), sinon pour un thésaurus complet, il faut juste l'id du thésaurus")
                    @RequestParam("idThesoConcept") List<String> idThesoConcepts,
            @Parameter(name = "limit", example = "true", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), required = false, description = "pour limiter ou non le nombre de concepts à récupérer")
            @RequestParam(value = "limit", required = false, defaultValue = "false") Boolean limit
    ) {
        try {
            List<IdValuePair> idValuePairs = new ArrayList<>();

            if (idThesoConcepts == null || idThesoConcepts.isEmpty()) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body("{\"nodes\":[],\"relationships\":[]}");
            }
            for (String idThesoConcept : idThesoConcepts) {
                String[] elements = idThesoConcept.split(":");

                if (elements.length == 2) {
                    IdValuePair idValuePair = new IdValuePair();
                    idValuePair.setIdTheso(elements[0]);
                    idValuePair.setIdConcept(elements[1]);
                    idValuePairs.add(idValuePair);
                }
                if (elements.length == 1) {
                    IdValuePair idValuePair = new IdValuePair();
                    idValuePair.setIdTheso(elements[0]);
                    idValuePairs.add(idValuePair);
                }
            }

            graphD3jsHelper.initGraph();

            boolean applyLimit = Boolean.TRUE.equals(limit);
            for (IdValuePair idValuePair : idValuePairs) {
                if (StringUtils.isEmpty(idValuePair.getIdConcept())) {
                    graphD3jsHelper.getGraphByTheso(idValuePair.getIdTheso(), lang, applyLimit);
                } else {
                    graphD3jsHelper.getGraphByConcept(
                            idValuePair.getIdTheso(), idValuePair.getIdConcept(), lang, applyLimit);
                }
            }

            var datas = graphD3jsHelper.getJsonFromNodeGraphD3js();
            if (datas == null) {
                datas = "{\"nodes\":[],\"relationships\":[]}";
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
        } catch (Exception e) {
            log.error("Erreur lors de la construction du graphe D3js pour {}", idThesoConcepts, e);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body("{\"nodes\":[],\"relationships\":[]}");
        }
    }
}

