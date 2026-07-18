package fr.cnrs.opentheso.v2.publicapi.thesaurus.api;

import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusFlatEntryResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusLanguagesResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusLastUpdateResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.api.dto.ThesaurusTopConceptResponse;
import fr.cnrs.opentheso.v2.publicapi.thesaurus.service.ThesaurusPublicReadService;
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

@RestController("v2PublicThesaurusController")
@RequestMapping("/openapi/v2/public/thesauri/{thesaurusId}")
@RequiredArgsConstructor
@Tag(name = "Thesaurus (public)", description = "Export et métadonnées publiques d'un thésaurus (v2, sans authentification)")
public class ThesaurusPublicController {

    private final ThesaurusPublicReadService thesaurusPublicReadService;

    @GetMapping(value = "/export")
    @Operation(summary = "Export SKOS complet d'un thésaurus", description = "Formats : skos (défaut), jsonld, turtle, json")
    public ResponseEntity<byte[]> exportThesaurus(
            @PathVariable String thesaurusId,
            @RequestParam(defaultValue = "skos") String format
    ) throws IOException {
        var result = thesaurusPublicReadService.exportThesaurus(thesaurusId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    @GetMapping(value = "/flatlist", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Liste plate id/label de tout le thésaurus")
    public List<ThesaurusFlatEntryResponse> flatList(
            @PathVariable String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        return thesaurusPublicReadService.flatList(thesaurusId, lang);
    }

    @GetMapping(value = "/topconcept", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Concepts racine (top concepts) avec traductions")
    public List<ThesaurusTopConceptResponse> topConcepts(@PathVariable String thesaurusId) {
        return thesaurusPublicReadService.topConcepts(thesaurusId);
    }

    @GetMapping(value = "/lastupdate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Date de dernière modification du thésaurus")
    public ThesaurusLastUpdateResponse lastUpdate(@PathVariable String thesaurusId) {
        return thesaurusPublicReadService.lastUpdate(thesaurusId);
    }

    @GetMapping(value = "/listlang", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Langues utilisées dans le thésaurus")
    public ThesaurusLanguagesResponse listLang(@PathVariable String thesaurusId) {
        return thesaurusPublicReadService.usedLanguages(thesaurusId);
    }
}
