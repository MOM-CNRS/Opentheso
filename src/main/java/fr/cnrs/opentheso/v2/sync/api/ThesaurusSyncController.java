package fr.cnrs.opentheso.v2.sync.api;

import fr.cnrs.opentheso.entites.User;
import fr.cnrs.opentheso.v2.shared.auth.ThesaurusWriteAuthorizationService;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncBatchResponse;
import fr.cnrs.opentheso.v2.sync.model.SyncChangesRequest;
import fr.cnrs.opentheso.v2.sync.model.SyncExportRequest;
import fr.cnrs.opentheso.v2.sync.service.ThesaurusSyncExportService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v2/thesaurus/{idThesaurus}/sync")
@RequiredArgsConstructor
@Tag(name = "Api v2 - Synchronisation")
public class ThesaurusSyncController {

    private final ThesaurusSyncReceiveService thesaurusSyncReceiveService;
    private final ThesaurusSyncExportService thesaurusSyncExportService;
    private final ThesaurusWriteAuthorizationService thesaurusWriteAuthorizationService;

    @PostMapping(
            path = "/concepts",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Synchronise un lot de concepts depuis un thésaurus esclave",
            description = "Compare chaque concept reçu au thésaurus maître : crée une proposition "
                    + "s'il existe ; crée un candidat s'il est inconnu et si createCandidates "
                    + "n'est pas false. Aucun changement n'est appliqué directement.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )
    public ResponseEntity<Object> syncConcepts(
            HttpServletRequest request,
            @PathVariable String idThesaurus,
            @RequestBody SyncBatchRequest body
    ) {
        User user = requireWriter(request, idThesaurus);
        try {
            SyncBatchResponse response = thesaurusSyncReceiveService.receiveBatch(idThesaurus, body, user);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            return textBadRequest(ex.getMessage());
        }
    }

    @PostMapping(
            path = "/changes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Liste les concepts du maître modifiés depuis une date",
            description = "Réservé au thésaurus maître. Si since est vide, tous les concepts sont listés.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )
    public ResponseEntity<Object> listChanges(
            HttpServletRequest request,
            @PathVariable String idThesaurus,
            @RequestBody(required = false) SyncChangesRequest body
    ) {
        requireWriter(request, idThesaurus);
        try {
            String since = body == null ? null : body.since();
            String lang = body == null ? null : body.lang();
            return ResponseEntity.ok(thesaurusSyncExportService.listChanges(idThesaurus, since, lang));
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return textBadRequest(ex.getMessage());
        }
    }

    @PostMapping(
            path = "/export",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Exporte le payload des concepts demandés",
            description = "Réservé au thésaurus maître. Sert à une copie pour importer propositions et candidats.",
            security = @SecurityRequirement(name = "ApiKeyAuth")
    )
    public ResponseEntity<Object> exportConcepts(
            HttpServletRequest request,
            @PathVariable String idThesaurus,
            @RequestBody SyncExportRequest body
    ) {
        requireWriter(request, idThesaurus);
        try {
            List<String> ids = body == null ? List.of() : body.conceptIds();
            String lang = body == null ? null : body.lang();
            return ResponseEntity.ok(thesaurusSyncExportService.exportConcepts(idThesaurus, ids, lang));
        } catch (IllegalStateException ex) {
            return textBadRequest(ex.getMessage());
        }
    }

    private User requireWriter(HttpServletRequest request, String idThesaurus) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            throw new ApiKeyInvalidException(ApiKeyState.INVALID);
        }
        if (!thesaurusWriteAuthorizationService.canUserWrite(user.getId(), idThesaurus)) {
            throw new UserCantWriteOnThesaurusException();
        }
        return user;
    }

    private static ResponseEntity<Object> textBadRequest(String message) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_PLAIN)
                .body(message);
    }
}
