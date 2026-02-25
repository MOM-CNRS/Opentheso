package fr.cnrs.opentheso.ws.openapi.v1.routes;

import fr.cnrs.opentheso.ws.api.RestRDFHelper;
import fr.cnrs.opentheso.ws.openapi.helper.CustomMediaType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/openapi/v1/redirect")
@CrossOrigin(methods = { RequestMethod.GET })
public class RedirectController {

    @Autowired
    private RestRDFHelper restRDFHelper;


    /**
     * permet de rediriger les URI ARK vers la bonne URL
     */
    @GetMapping(
            value = {
                    "/ark:/{naan}/{idArk}",   // ark:/naan/id
                    "/ark:{naan}/{idArk}"     // ark:naan/id
            },
            produces = CustomMediaType.APPLICATION_JSON_UTF_8
    )
    @Operation(summary = "Redirige vers la page de la ressource correspondant à l'identifiant ARK entré",
            description = "Ancienne version : `/api/ark:/{naan}/{idArk}`\\n\\nRedirige vers la page de la ressource correspondant à l'identifiant ARK entré",
            tags = {"Ark"},
            responses = {
                @ApiResponse(responseCode = "200", description = "Page de la ressource demandé après redirection"),
                @ApiResponse(responseCode = "307", description = "Redirection vers la page de la ressource correspondante"),
                @ApiResponse(responseCode = "400", description = "Erreur dans la synthaxe de la requête"),
                @ApiResponse(responseCode = "404", description = "Aucune ressource n'est associé à cet identifiant ARK", content = { @Content(mediaType = CustomMediaType.APPLICATION_JSON_UTF_8) }),
                @ApiResponse(responseCode = "500", description = "Erreur interne du serveur")
            })
    public ResponseEntity<Object> getUriFromArk(@PathVariable("naan") String naan, @PathVariable("idArk") String arkId) throws URISyntaxException {

        var webUrl = restRDFHelper.getUrlFromIdArk(naan, arkId);
        return ResponseEntity.status(307).location(new URI(webUrl)).build();
    }


    /**
     * permet de rediriger une URI Opentheso vers un concept dans le thésaurus, permet d'éviter l'utilisaton des (&)
     */
    @GetMapping(value = "/{idTheso}/{idConcept}", produces = {CustomMediaType.APPLICATION_JSON_UTF_8})
    @Operation(summary = "Permet de rediriger vers un concept dans un thésaurus donné",
            description = "",
            tags = {"Concept"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Une URL de redirection"),
                    @ApiResponse(responseCode = "400", description = "Erreur dans la synthaxe de la requête"),
                    @ApiResponse(responseCode = "404", description = "Aucun concept n'existe avec cet ID dans le thesaurus choisi"),
                    @ApiResponse(responseCode = "503", description = "Pas de connexion au serveur")
            })
    public ResponseEntity<Object> redirectToConcept(@Parameter(name = "idTheso", description = "ID du thesaurus dans lequel récupérer le concept.", required = true) @PathVariable("idTheso") String idThesaurus,
                                                       @Parameter(name = "idConcept", description = "Identifiant du concept à récupérer.", required = true) @PathVariable("idConcept") String idConcept,
                                                       HttpServletRequest request) throws URISyntaxException  {
        // Récupération de l'URL de la requête
        String requestUrl = request.getRequestURL().toString();
        String newUrl = requestUrl.replace("/openapi/v1/redirect/" +idThesaurus + "/" + idConcept, "/") + "?idc=" + idConcept + "&idt=" + idThesaurus;
        return ResponseEntity.status(307).location(new URI(newUrl)).build();
    }

    /**
     * permet de rediriger une URI Opentheso vers un thésaurus, permet d'éviter l'utilisaton des (&)
     */
    @GetMapping(value = "/{idTheso}", produces = {CustomMediaType.APPLICATION_JSON_UTF_8})
    @Operation(summary = "Permet de rediriger vers un thésaurus donné",
            description = "",
            tags = {"Concept"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Une URL de redirection"),
                    @ApiResponse(responseCode = "400", description = "Erreur dans la synthaxe de la requête"),
                    @ApiResponse(responseCode = "404", description = "Aucun concept n'existe avec cet ID dans le thesaurus choisi"),
                    @ApiResponse(responseCode = "503", description = "Pas de connexion au serveur")
            })
    public ResponseEntity<Object> redirectToTheso(@Parameter(name = "idTheso", description = "ID du thesaurus dans lequel récupérer le concept.", required = true) @PathVariable("idTheso") String idThesaurus,
                                                  HttpServletRequest request) throws URISyntaxException  {
        // Récupération de l'URL de la requête
        String requestUrl = request.getRequestURL().toString();
        String newUrl = requestUrl.replace("/openapi/v1/redirect/" +idThesaurus, "/") + "?idt=" + idThesaurus;
        return ResponseEntity.status(307).location(new URI(newUrl)).build();
    }



}
