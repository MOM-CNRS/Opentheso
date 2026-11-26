package fr.cnrs.opentheso.ws.openapi.v1.routes.concept;


import fr.cnrs.opentheso.setting.service.ThesaurusApiSwitch;
import fr.cnrs.opentheso.ws.api.RestThesaurusAccess;import fr.cnrs.opentheso.models.propositions.PropositionFromApi;
import fr.cnrs.opentheso.services.PropositionService;
import fr.cnrs.opentheso.services.UserService;
import fr.cnrs.opentheso.services.ApiKeyService;
import fr.cnrs.opentheso.utils.MD5Password;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
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
@RequestMapping("/openapi/v1/concepts/propositions")
@CrossOrigin(methods = { RequestMethod.POST })
@Tag(name = "Proposition", description = "Ajouter une proposition")
public class PropositionsController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;
    private final PropositionService propositionService;
    private final ThesaurusApiSwitch thesaurusApiSwitch;


    @PostMapping(consumes = MediaType.APPLICATION_JSON)
    public ResponseEntity<Object> createProposition(@RequestHeader(value = "API-KEY") String apiKey,
                                                    @RequestBody PropositionFromApi proposition) {

        var keyState = apiKeyService.checkApiKey(apiKey);

        if (keyState != ApiKeyState.VALID) {
            return apiKeyService.errorResponse(keyState);
        }

        if (thesaurusApiSwitch.isDisabled(proposition.getIdTheso())) {
            return ResponseEntity.status(RestThesaurusAccess.API_DISABLED.status())
                    .body(RestThesaurusAccess.API_DISABLED.body());
        }

        var user = userService.getUserByApiKey(MD5Password.getEncodedPassword(apiKey));
        propositionService.createProposition(proposition, user);
        return ResponseEntity.status(201).build();
    }
}
