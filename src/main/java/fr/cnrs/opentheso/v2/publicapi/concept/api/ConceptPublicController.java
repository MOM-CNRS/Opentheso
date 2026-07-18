package fr.cnrs.opentheso.v2.publicapi.concept.api;

import fr.cnrs.opentheso.v2.publicapi.concept.api.dto.OntomeLinkedConceptResponse;
import fr.cnrs.opentheso.v2.publicapi.concept.service.ConceptPublicExportService;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptLabelResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptRelationResponse;
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

@RestController("v2PublicConceptController")
@RequestMapping("/openapi/v2/public/thesauri/{thesaurusId}/concepts")
@RequiredArgsConstructor
@Tag(name = "Concepts (public)", description = "Export et lecture publique des concepts (v2, sans authentification)")
public class ConceptPublicController {

    private final ConceptPublicExportService conceptPublicExportService;

    @GetMapping(value = "/{conceptId}/export")
    @Operation(summary = "Export SKOS d'un concept", description = "Formats : skos (défaut), jsonld, turtle, json")
    public ResponseEntity<byte[]> exportConcept(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = conceptPublicExportService.exportConcept(thesaurusId, conceptId, format);
        return toFileResponse(result.content(), result.filename(), result.contentType());
    }

    @GetMapping(value = "/{conceptId}/labels", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Libellés d'un concept", description = "prefLabel et altLabels toutes langues")
    public List<ConceptLabelResponse> loadLabels(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        return conceptPublicExportService.loadLabels(thesaurusId, conceptId, lang);
    }

    @GetMapping(value = "/{conceptId}/narrower", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Termes spécifiques (NT) d'un concept")
    public List<ConceptRelationResponse> loadNarrower(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        return conceptPublicExportService.loadNarrower(thesaurusId, conceptId, lang);
    }

    @GetMapping(value = "/{conceptId}/expansion")
    @Operation(summary = "Export d'une branche entière", description = "way=top (ancêtres) ou down (descendants)")
    public ResponseEntity<byte[]> exportExpansion(
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(defaultValue = "down") String way,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = conceptPublicExportService.exportExpansion(thesaurusId, conceptId, way, format);
        return toFileResponse(result.content(), result.filename(), result.contentType());
    }

    @GetMapping(value = "/modified-since/{date}")
    @Operation(summary = "Export des concepts modifiés depuis une date", description = "date au format ISO (yyyy-MM-dd)")
    public ResponseEntity<byte[]> exportModifiedSince(
            @PathVariable String thesaurusId,
            @PathVariable String date,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = conceptPublicExportService.exportModifiedSince(thesaurusId, date, format);
        return toFileResponse(result.content(), result.filename(), result.contentType());
    }

    @GetMapping(value = "/ontome", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Concepts alignés avec Ontome/CIDOC-CRM", description = "cidocClass optionnel : sans, renvoie tous les alignements Ontome")
    public List<OntomeLinkedConceptResponse> loadOntomeLinkedConcepts(
            @PathVariable String thesaurusId,
            @RequestParam(required = false) String cidocClass
    ) {
        return conceptPublicExportService.loadOntomeLinkedConcepts(thesaurusId, cidocClass);
    }

    private ResponseEntity<byte[]> toFileResponse(byte[] content, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(content);
    }
}
