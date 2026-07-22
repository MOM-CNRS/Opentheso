package fr.cnrs.opentheso.ws.openapi.v1.routes.thesaurus;

import fr.cnrs.opentheso.services.PreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.Json;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static fr.cnrs.opentheso.ws.openapi.helper.CustomMediaType.*;


@Slf4j
@RestController
@RequestMapping("/openapi/v1/thesaurus-resolve/{persistentThesaurusName}")
@CrossOrigin(methods = { RequestMethod.GET })
@Tag(name = "Thesaurus", description = "Contient toutes les actions en liens avec les thesaurus.")
public class ThesaurusNameController {

    @Autowired
    private PreferenceService preferenceService;


    /**
     * permet de récupérer l'Id du thésaurus d'après son Nom Pérenne
     */
    @GetMapping(produces = {APPLICATION_JSON_UTF_8})
    @Operation(summary = "Permet de récupérer l'identifiant du thesaurus d'après son Nom Pérenne",
            description = "Permet de récupérer l'identifiant du thesaurus d'après son Nom Pérenne, le Nom prérenne se trouve dans les préférences.",
            tags = {"Thesaurus"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "ID du thesaurus.", content = { @Content(mediaType = APPLICATION_JSON_UTF_8) }),
                    @ApiResponse(responseCode = "404", description = "Thésaurus non trouvé")
            }
    )
    public ResponseEntity<Object> getThesaurusId(@Parameter(name = "persistentThesaurusName",
            description = "Nom pérenne du thésaurus.", required = true) @PathVariable("persistentThesaurusName") String persistentThesaurusName) {

        String idTheso = preferenceService.getThesaurusIdFromPersistentName(persistentThesaurusName);

        if (idTheso == null) {

            var error = Json.createObjectBuilder()
                    .add("code", "THESAURUS_NOT_FOUND")
                    .add("message", "Aucun thésaurus trouvé pour le nom pérenne fourni.")
                    .build();

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(error.toString());
        }
        var job = Json.createObjectBuilder();
        job.add("idTheso", idTheso);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(job.build().toString());
    }


}