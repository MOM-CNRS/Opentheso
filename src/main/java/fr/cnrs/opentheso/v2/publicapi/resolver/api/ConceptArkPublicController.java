package fr.cnrs.opentheso.v2.publicapi.resolver.api;

import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ArkFullPathResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ConceptChildrenArkResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.api.dto.ConceptPrefLabelResponse;
import fr.cnrs.opentheso.v2.publicapi.resolver.service.ConceptArkPublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController("v2PublicConceptArkController")
@RequestMapping("/openapi/v2/public/concepts")
@RequiredArgsConstructor
@Tag(name = "Concepts ARK/Handle (public)", description = "Résolution de concepts par identifiant ARK ou Handle (v2, sans authentification)")
public class ConceptArkPublicController {

    private final ConceptArkPublicService conceptArkPublicService;

    @GetMapping("/ark/{naan}/{arkId}/export")
    @Operation(summary = "Export SKOS d'un concept identifié par son ARK")
    public ResponseEntity<byte[]> exportByArk(
            @PathVariable String naan,
            @PathVariable String arkId,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = conceptArkPublicService.exportByArk(naan, arkId, format);
        return toFileResponse(result.content(), result.filename(), result.contentType());
    }

    @GetMapping(value = "/ark/{naan}/{arkId}/childs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Identifiants ARK des concepts enfants")
    public ConceptChildrenArkResponse loadChildrenArkIds(
            @PathVariable String naan,
            @PathVariable String arkId
    ) {
        return conceptArkPublicService.loadChildrenArkIds(naan, arkId);
    }

    @GetMapping(value = "/ark/{naan}/{arkId}/pref-label/{lang}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Libellé préférentiel d'un concept identifié par son ARK")
    public ConceptPrefLabelResponse loadPrefLabel(
            @PathVariable String naan,
            @PathVariable String arkId,
            @PathVariable String lang
    ) {
        return conceptArkPublicService.loadPrefLabel(naan, arkId, lang);
    }

    @GetMapping("/handle/{handle}/{idHandle}/export")
    @Operation(summary = "Export SKOS d'un concept identifié par son Handle")
    public ResponseEntity<byte[]> exportByHandle(
            @PathVariable String handle,
            @PathVariable String idHandle,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = conceptArkPublicService.exportByHandle(handle, idHandle, format);
        return toFileResponse(result.content(), result.filename(), result.contentType());
    }

    @GetMapping(value = "/ark/fullpath", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fil d'Ariane pour une liste d'identifiants ARK")
    public List<ArkFullPathResponse> fullPathByArk(
            @RequestParam List<String> arkIds,
            @RequestParam(required = false) String lang
    ) {
        return conceptArkPublicService.fullPathByArk(arkIds, lang);
    }

    private ResponseEntity<byte[]> toFileResponse(byte[] content, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
