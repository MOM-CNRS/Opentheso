package fr.cnrs.opentheso.ws.openapi.v1.routes.thesaurus;

import fr.cnrs.opentheso.entites.ThesaurusDcTerm;
import fr.cnrs.opentheso.models.thesaurus.Thesaurus;

import fr.cnrs.opentheso.services.ThesaurusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static fr.cnrs.opentheso.ws.openapi.helper.CustomMediaType.APPLICATION_JSON_UTF_8;

/**
 *
 * @author julie
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/openapi/v1/thesaurus")
@CrossOrigin(methods = { RequestMethod.GET })
@Tag(name = "Thesaurus", description = "Contient toutes les actions en liens avec les thesaurus.")
public class ThesaurusController {

    private final ThesaurusService thesaurusService;


    @GetMapping
    @Operation(summary = "Permet de récupérer tous les thesaurus publiques",
            description = "Ancienne version : `/api/info/list?theso=all`<br/>Récupère une liste de tous les thésaurus publiques ainsi que les langues dans lesquelles ils sont disponibles.",
            tags = {"Thesaurus"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Liste contenant les ID des thesaurus publiques.", content = { @Content(mediaType = APPLICATION_JSON_UTF_8) }),
                    @ApiResponse(responseCode = "404", description = "Thésaurus non trouvé")
            }
    )
    public ResponseEntity<Object> getListAllPublicTheso() {

        var listPublicTheso = thesaurusService.getAllIdOfThesaurus(false);

        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        for (String idTheso : listPublicTheso) {
            var job = Json.createObjectBuilder();
            job.add("idTheso", idTheso);
            var jsonArrayBuilderLang = Json.createArrayBuilder();

            // pour indiquer le type du thésaurus (exemple pour Siamois ...)
            List<ThesaurusDcTerm> thesaurusDcTerms = thesaurusService.getDcTermsOfThesaurus(idTheso);
            String type = "";
            for (ThesaurusDcTerm term : thesaurusDcTerms) {
                if ("type".equalsIgnoreCase(term.getName())) {
                    type = term.getValue();
                    break; // on sort dès qu'on a trouvé
                }
            }
            job.add("type", type);

            var nodeThesaurus = thesaurusService.getNodeThesaurus(idTheso);
            for (Thesaurus thesaurus : nodeThesaurus.getListThesaurusTraduction()) {
                var jobLang = Json.createObjectBuilder();
                jobLang.add("lang", thesaurus.getLanguage());
                jobLang.add("title", thesaurus.getTitle());
                jsonArrayBuilderLang.add(jobLang.build());
            }
            if (!nodeThesaurus.getListThesaurusTraduction().isEmpty()) {
                job.add("labels", jsonArrayBuilderLang.build());
            }

            jsonArrayBuilder.add(job.build());
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(jsonArrayBuilder.build().toString());
    }
}
