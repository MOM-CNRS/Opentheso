package fr.cnrs.opentheso.v2.sync.api;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusWriteAuthorizationService;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncReceiveService;
import fr.cnrs.opentheso.ws.openapi.exception.ApiKeyInvalidException;
import fr.cnrs.opentheso.ws.openapi.exception.UserCantWriteOnThesaurusException;
import fr.cnrs.opentheso.ws.openapi.helper.ApiKeyState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/thesaurus/{idThesaurus}/sync/concepts")
@RequiredArgsConstructor
@Tag(name = "Api v2 - Synchronisation")
public class ThesaurusSyncController {

    private final ThesaurusSyncReceiveService thesaurusSyncReceiveService;
    private final ThesaurusWriteAuthorizationService thesaurusWriteAuthorizationService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Synchronise un lot de concepts depuis un thésaurus esclave",
            description = "Compare chaque concept reçu au thésaurus maître : crée une proposition "
                    + "s'il existe, un candidat sinon. Aucun changement n'est appliqué directement.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )
    public ResponseEntity<?> syncConcepts(
            HttpServletRequest request,
            @PathVariable String idThesaurus,
            @RequestBody SyncBatchRequest body
    ) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            throw new ApiKeyInvalidException(ApiKeyState.INVALID);
        }
        if (!thesaurusWriteAuthorizationService.canUserWrite(user.getId(), idThesaurus)) {
            throw new UserCantWriteOnThesaurusException();
        }
        try {
            SyncBatchResponse response = thesaurusSyncReceiveService.receiveBatch(idThesaurus, body, user);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            // Corps texte pour que l'esclave affiche la raison (ex. thésaurus non maître).
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(ex.getMessage());
        }
    }
}
