package fr.cnrs.opentheso.ws.openapi.v1.routes.concept;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnrs.opentheso.services.GroupService;
import fr.cnrs.opentheso.services.SearchService;
import fr.cnrs.opentheso.ws.api.RestRDFHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static fr.cnrs.opentheso.ws.openapi.helper.CustomMediaType.APPLICATION_JSON_UTF_8;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/openapi/v1/concept/{idTheso}/autocomplete")
@CrossOrigin(methods = { RequestMethod.GET })
@Tag(name = "Concept", description = "Contient toutes les actions disponibles sur les concepts.")
public class ConceptAutocompleteController {

    private final RestRDFHelper restRDFHelper;
    private final GroupService groupService;
    private final SearchService searchService;


    @GetMapping(value = "/{input}", produces = APPLICATION_JSON_UTF_8)
    @Operation(summary = "Recherche les termes proches du terme en entré",
            description = "Ancienne version : `/api/autocomplete?input=<input>&theso=<idTheso>` ou `/api/autocomplete?value=<input>&theso=<idTheso>`<br/>Permet de  récupérer les termes proches du terme entré pour ainsi pouvoir effectuer de l'auto-complétion avec possibilité de filtrer par langue et groupe",
            tags = {"Concept"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fichier contenant les termes proches de l'entrée", content = { @Content(mediaType = APPLICATION_JSON_UTF_8) }),
                    @ApiResponse(responseCode = "400", description = "Erreur dans la synthaxe de la requête"),
                    @ApiResponse(responseCode = "404", description = "${searchAutocomplete.404.description}$")
            })
    public ResponseEntity<Object> searchAutocomplete(
            @Parameter(name = "idTheso", required = true, description = "Thésaurus dans lequel chercher la saisie de l'utilisateur") @PathVariable("idTheso") String idTheso,
            @Parameter(name = "input", required = true, description = "Saisie de l'utilisateur") @PathVariable("input") String input,
            @Parameter(name = "lang", in = ParameterIn.QUERY, schema = @Schema(type = "string"), description = "Langue dans laquelle chercher la saisie de l'utilisateur") @RequestParam(value = "lang", required = false) String lang,
            @Parameter(name = "group", in = ParameterIn.QUERY, schema = @Schema(type = "string"), description = "Groupe dans lequel chercher la saisie de l'utilisateur") @RequestParam(value = "group", required = false) String groupsString,
            @Parameter(name = "full", in = ParameterIn.QUERY, schema = @Schema(type = "boolean"), description = "`true` si l'on souhaite retourner plus d'informations sur le concept. Les informations supplémentaires sont le `prefLabel`, `altLabel` et la définition du concept") @RequestParam(value = "full", required = false) String fullString) {

        var groups = groupsString != null ? groupsString.split(",") : null;
        var full = fullString != null && fullString.equalsIgnoreCase("true");
        input = URLDecoder.decode(input, StandardCharsets.UTF_8);
        var datas = restRDFHelper.findAutocompleteConcepts(idTheso, lang, groups, input, full);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(datas);
    }

    @GetMapping("/{input}/full")
    public ResponseEntity<Object> searchAutocompleteV2(@PathVariable("idTheso") String idTheso,
                                               @PathVariable("input") String input,
                                               @RequestParam("lang") String lang,
                                               @RequestParam("group") String idGroup) throws JsonProcessingException {

        var concepts = searchService.searchConceptWSV2(input, lang, idGroup, idTheso);
        var jsonString = new ObjectMapper().writeValueAsString(concepts);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonString);
    }

    @GetMapping("/{idThesaurus}/{idLang}")
    public ResponseEntity<Object> getGroupsByThesaurus(@PathVariable("idThesaurus") String idThesaurus,
                                               @PathVariable("idLang") String idLang) throws JsonProcessingException {

        var groups = groupService.getListRootConceptGroup(idThesaurus, idLang, true, false);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new ObjectMapper().writeValueAsString(groups));
    }
}
