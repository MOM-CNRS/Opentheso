package fr.cnrs.opentheso.ws.openapi.v1.routes.conceptpost;


import fr.cnrs.opentheso.setting.service.ThesaurusApiSwitch;
import fr.cnrs.opentheso.ws.api.RestThesaurusAccess;import fr.cnrs.opentheso.candidat.api.CandidatAuthSupport;
import fr.cnrs.opentheso.services.CandidatService;
import fr.cnrs.opentheso.services.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final UserService userService;
    private final CandidatAuthSupport candidatAuthSupport;
    private final ThesaurusApiSwitch thesaurusApiSwitch;


    /**
     * Route qui permet d'ajouter un candidat à partir d'un JSON
     */
    @PostMapping
    @Operation(summary = "Permet d'ajouter un candidat",
            description = "Ajoute un candidat. Refuse la création si un libellé préféré identique existe déjà "
                    + "dans le thésaurus. Le tableau notes accepte les types definition, note, scopeNote, "
                    + "historyNote, editorialNote, changeNote et example.",
            tags = {"Concept Write"},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON représentant le candidat à ajouter"),
            security = { @SecurityRequirement(name = "API-KEY") })
    public ResponseEntity addCandidate(@RequestHeader(value = "API-KEY") String apiKey, @RequestBody Candidate candidate) {
        var user = userService.getUserByApiKeyBcrypt(apiKey);
        if (user == null) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("Clé d'API invalide");
        }
        if (candidate != null && thesaurusApiSwitch.isDisabled(candidate.getThesoId())) {
            return ResponseEntity.status(RestThesaurusAccess.API_DISABLED.status())
                    .contentType(MediaType.APPLICATION_JSON).body(RestThesaurusAccess.API_DISABLED.body());
        }
        candidatAuthSupport.requireContributor(user.getId(), candidate.getThesoId());
        String unknownNoteType = candidatService.unsupportedNoteType(candidate);
        if (unknownNoteType != null) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body("Type de note inconnu : " + unknownNoteType);
        }
        if (candidate != null && candidate.getTerme() != null) {
            for (Element term : candidate.getTerme()) {
                if (term != null && term.getValue() != null
                        && !term.getValue().isBlank()
                        && candidatService.preferredLabelExists(candidate.getThesoId(), term.getValue())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                            .body("Un libellé préféré identique existe déjà dans ce thésaurus");
                }
            }
        }
        if (!candidatService.saveCandidat(candidate, user.getId())) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body("");
        }
        return ResponseEntity.ok().body("");
    }

}
