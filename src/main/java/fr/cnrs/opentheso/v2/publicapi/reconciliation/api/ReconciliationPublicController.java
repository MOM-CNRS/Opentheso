package fr.cnrs.opentheso.v2.publicapi.reconciliation.api;

import fr.cnrs.opentheso.v2.publicapi.reconciliation.service.ReconciliationPublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController("v2PublicReconciliationController")
@RequestMapping("/openapi/v2/public/reconciliation")
@RequiredArgsConstructor
@Tag(name = "Réconciliation OpenRefine (public)", description = "Service de réconciliation OpenRefine (v2, sans authentification)")
public class ReconciliationPublicController {

    private final ReconciliationPublicService reconciliationPublicService;

    @GetMapping(value = "/{thesaurusId}/{lang}/manifest", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Manifeste du service de réconciliation OpenRefine")
    public Map<String, Object> manifest(
            @PathVariable String thesaurusId,
            @PathVariable String lang,
            HttpServletRequest request
    ) {
        return reconciliationPublicService.metadata(baseUrl(request), thesaurusId, lang);
    }

    @PostMapping(value = "/{thesaurusId}/{lang}/reconcile", consumes = "application/x-www-form-urlencoded")
    @Operation(summary = "Réconciliation par lot de requêtes OpenRefine")
    public Map<String, Object> reconcile(
            @PathVariable String thesaurusId,
            @PathVariable String lang,
            @RequestParam String queries,
            HttpServletRequest request
    ) throws Exception {
        return reconciliationPublicService.reconcile(baseUrl(request), thesaurusId, lang, queries);
    }

    @PostMapping(value = "/{thesaurusId}/{lang}/reconcile", params = "extend", consumes = "application/x-www-form-urlencoded")
    @Operation(summary = "Enrichissement des concepts réconciliés (OpenRefine extend)")
    public Map<String, Object> extend(
            @PathVariable String thesaurusId,
            @PathVariable String lang,
            @RequestParam String extend
    ) throws Exception {
        return reconciliationPublicService.extend(thesaurusId, lang, extend);
    }

    @GetMapping(value = "/{thesaurusId}/{lang}/suggest/entity", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Autocomplétion d'entités pour OpenRefine")
    public Map<String, Object> suggestEntity(
            @PathVariable String thesaurusId,
            @PathVariable String lang,
            @RequestParam String prefix,
            HttpServletRequest request
    ) {
        return reconciliationPublicService.suggestEntity(baseUrl(request), thesaurusId, lang, prefix);
    }

    @GetMapping(value = "/suggest/properties", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liste des propriétés utilisables pour la réconciliation")
    public Map<String, Object> suggestProperties() {
        return reconciliationPublicService.suggestProperties();
    }

    @GetMapping(value = "/propose-properties", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Propriétés proposées pour l'enrichissement OpenRefine")
    public Map<String, Object> proposeProperties() {
        return reconciliationPublicService.proposeProperties();
    }

    @GetMapping(value = "/preview/{thesaurusId}/{conceptId}", produces = "text/html")
    @Operation(summary = "Aperçu HTML d'un concept (survol OpenRefine)")
    public ResponseEntity<String> preview(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId
    ) {
        return ResponseEntity.ok()
                .header("Content-Security-Policy", "frame-ancestors 'self' *")
                .body(reconciliationPublicService.preview(thesaurusId, conceptId));
    }

    private String baseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        String authority = isDefaultPort ? host : host + ":" + port;
        return scheme + "://" + authority + request.getContextPath();
    }
}
