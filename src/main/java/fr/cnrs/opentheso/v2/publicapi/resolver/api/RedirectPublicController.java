package fr.cnrs.opentheso.v2.publicapi.resolver.api;

import fr.cnrs.opentheso.v2.publicapi.exception.PublicResourceNotFoundException;
import fr.cnrs.opentheso.v2.publicapi.resolver.service.ArkRedirectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;

@RestController("v2PublicRedirectController")
@RequestMapping("/openapi/v2/public/redirect")
@RequiredArgsConstructor
@Tag(name = "Redirections (public)", description = "Résolution ARK et redirections courtes (v2, sans authentification)")
public class RedirectPublicController {

    private final ArkRedirectService arkRedirectService;

    @GetMapping({"/ark:/{naan}/{idArk}", "/ark:{naan}/{idArk}"})
    @Operation(summary = "Redirige vers la ressource correspondant à un identifiant ARK")
    public ResponseEntity<Object> redirectFromArk(
            @PathVariable String naan,
            @PathVariable String idArk
    ) throws URISyntaxException {
        String url = arkRedirectService.buildRedirectUrl(naan, idArk)
                .orElseThrow(() -> new PublicResourceNotFoundException("Aucune ressource pour l'identifiant ARK : " + naan + "/" + idArk));
        return ResponseEntity.status(307).location(new URI(url)).build();
    }

    @GetMapping("/{thesaurusId}/{conceptId}")
    @Operation(summary = "Redirige vers un concept dans un thésaurus donné")
    public ResponseEntity<Object> redirectToConcept(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            HttpServletRequest request
    ) throws URISyntaxException {
        String requestUrl = request.getRequestURL().toString();
        String newUrl = requestUrl.replace("/openapi/v2/public/redirect/" + thesaurusId + "/" + conceptId, "/")
                + "?idc=" + conceptId + "&idt=" + thesaurusId;
        return ResponseEntity.status(307).location(new URI(newUrl)).build();
    }

    @GetMapping("/{thesaurusId}")
    @Operation(summary = "Redirige vers un thésaurus donné")
    public ResponseEntity<Object> redirectToThesaurus(
            @PathVariable String thesaurusId,
            HttpServletRequest request
    ) throws URISyntaxException {
        String requestUrl = request.getRequestURL().toString();
        String newUrl = requestUrl.replace("/openapi/v2/public/redirect/" + thesaurusId, "/") + "?idt=" + thesaurusId;
        return ResponseEntity.status(307).location(new URI(newUrl)).build();
    }
}
