package fr.cnrs.opentheso.ws.openapi.v2;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.models.skos.SkosConceptUpdateDto;
import fr.cnrs.opentheso.skos.api.OpenApiConceptUpdateOperations;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusWriteAuthorizationService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.UserCantWriteOnThesaurusException;
import org.springframework.http.MediaType;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v2/thesaurus/{idThesaurus}/concepts")
@RequiredArgsConstructor
@Tag(name = "Api v2")
public class UpdateConceptController {

    private final OpenApiConceptUpdateOperations openApiConceptUpdateOperations;
    private final ThesaurusWriteAuthorizationService thesaurusWriteAuthorizationService;

    @PutMapping(
            value = "/{idConcept}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Met à jour un Concept",
            description = "Met à jour complètement un concept existant dans le thésaurus.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )
    public ResponseEntity<SkosConceptUpdateDto> updateConcept(
            HttpServletRequest requestHeader,
            @PathVariable String idThesaurus,
            @PathVariable String idConcept,
            @Valid @RequestBody SkosConceptUpdateDto dto
    ) {
        // Authentification
        User user = (User) requestHeader.getAttribute("authenticatedUser");
        if (user == null) {
            throw new ApiKeyInvalidException(ApiKeyState.INVALID);
        }

        if (!thesaurusWriteAuthorizationService.canUserWrite(user.getId(), idThesaurus)) {
            throw new UserCantWriteOnThesaurusException();
        }

        if (!openApiConceptUpdateOperations.exists(idConcept, idThesaurus)) {
            throw new RuntimeException("Ce concept n'existe pas dans le thésaurus ! : " + idConcept);
        }

        SkosConceptUpdateDto updated = openApiConceptUpdateOperations.updateConcept(
                dto,
                idThesaurus,
                idConcept,
                user.getId()
        );

        return ResponseEntity.ok(updated);
    }
}
