package fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost;

import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;



@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/openapi/v1/candidate")
@CrossOrigin(methods = { RequestMethod.POST })
@Tag(name = "Candidat", description = "Ajouter des candidats")
public class CandidatController {

    private final CandidatService candidatService;
    private final ApiKeyService apiKeyHelper;
    private final UserService userService;


    /**
     * Route qui permet d'ajouter un candidat à partir d'un JSON
     */
    @PostMapping
    @Operation(summary = "Permet d'ajouter un candidat",
            description = "Permet d'ajouter un candidat à partir du JSON donné",
            tags = {"Concept Write"},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON représentant le candidat à ajouter"),
            security = { @SecurityRequirement(name = "API-KEY") })
    public ResponseEntity addCandidate(@RequestHeader(value = "API-KEY") String apiKey, @RequestBody Candidate candidate) {
        var user = userService.getUserByApiKeyBcrypt(apiKey);
        if (user == null) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("Clé d'API invalide");
        }
        if (!candidatService.saveCandidat(candidate, user.getId())) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("");
        }
        return ResponseEntity.ok().body("");
    }

}
