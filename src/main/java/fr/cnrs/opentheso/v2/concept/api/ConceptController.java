package fr.cnrs.opentheso.v2.concept.api;

import fr.cnrs.opentheso.v2.concept.api.dto.ConceptBreadcrumbResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptDetailResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptIndexSearchResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptSearchResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptSummaryResponse;
import fr.cnrs.opentheso.v2.concept.api.dto.ConceptTreeNodeResponse;
import fr.cnrs.opentheso.v2.concept.api.mapper.ConceptApiMapper;
import fr.cnrs.opentheso.v2.concept.export.service.ConceptSkosExportService;
import fr.cnrs.opentheso.v2.concept.service.ConceptReadService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusWorkLanguageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import fr.cnrs.opentheso.v2.shared.api.ApiHeaders;

@RestController("v2ConceptController")
@RequestMapping("/openapi/v2/thesauri/{thesaurusId}/concepts")
@RequiredArgsConstructor
@Tag(name = "Concepts", description = "Lecture des concepts et de l'arbre thésaurus (v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class ConceptController {

    private final ConceptAuthSupport conceptAuthSupport;
    private final ConceptReadService conceptReadService;
    private final ConceptSkosExportService conceptSkosExportService;
    private final ThesaurusWorkLanguageService thesaurusWorkLanguageService;

    @GetMapping(value = "/tree/roots", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Racines de l'arbre", description = "Groupes ou concepts top sans groupe.")
    public List<ConceptTreeNodeResponse> loadRootNodes(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam(required = false) String lang
    ) {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        String workLang = resolveLanguage(thesaurusId, lang);
        return conceptReadService.loadRootNodes(thesaurusId, workLang).stream()
                .map(ConceptApiMapper::toTreeNode)
                .toList();
    }

    @GetMapping(value = "/tree/{parentId}/children", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Enfants d'un nœud", description = "Concepts enfants ou top d'un groupe.")
    public List<ConceptTreeNodeResponse> loadChildNodes(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String parentId,
            @RequestParam String parentType,
            @RequestParam(required = false) String lang
    ) {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        String workLang = resolveLanguage(thesaurusId, lang);
        return conceptReadService.loadChildNodes(parentId, parentType, thesaurusId, workLang).stream()
                .map(ConceptApiMapper::toTreeNode)
                .toList();
    }

    @GetMapping(value = "/{conceptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Résumé d'un concept")
    public ConceptSummaryResponse loadSummary(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        String workLang = resolveLanguage(thesaurusId, lang);
        return conceptReadService.loadSummary(thesaurusId, conceptId, workLang)
                .map(ConceptApiMapper::toSummary)
                .orElse(null);
    }

    @GetMapping(value = "/{conceptId}/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fiche complète d'un concept")
    public ConceptDetailResponse loadDetail(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        String workLang = resolveLanguage(thesaurusId, lang);
        return conceptReadService.loadDetail(thesaurusId, conceptId, workLang)
                .map(ConceptApiMapper::toDetail)
                .orElse(null);
    }

    @GetMapping(value = "/{conceptId}/breadcrumb", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fil d'Ariane d'un concept")
    public List<ConceptBreadcrumbResponse> loadBreadcrumb(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        String workLang = resolveLanguage(thesaurusId, lang);
        return conceptReadService.loadBreadcrumb(thesaurusId, conceptId, workLang).stream()
                .map(ConceptApiMapper::toBreadcrumb)
                .toList();
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recherche par libellé")
    public ConceptSearchResponse search(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam String q,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(required = false) String lang
    ) {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        String workLang = resolveLanguage(thesaurusId, lang);
        var results = conceptReadService.searchByLabel(thesaurusId, workLang, q, limit).stream()
                .map(ConceptApiMapper::toTreeNode)
                .toList();
        return new ConceptSearchResponse(q, results);
    }

    @GetMapping(value = "/index-search", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Recherche index alphabétique")
    public ConceptIndexSearchResponse searchIndex(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam String q,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "false") boolean permuted,
            @RequestParam(defaultValue = "false") boolean withAltLabel,
            @RequestParam(required = false) String lang
    ) {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        String workLang = resolveLanguage(thesaurusId, lang);
        var results = conceptReadService.searchIndex(thesaurusId, workLang, q, permuted, withAltLabel, limit).stream()
                .map(ConceptApiMapper::toTreeNode)
                .toList();
        return new ConceptIndexSearchResponse(q, permuted, withAltLabel, results);
    }

    @GetMapping(value = "/{conceptId}/export")
    @Operation(summary = "Export SKOS d'un concept", description = "Formats : skos (défaut), jsonld, turtle, json")
    public ResponseEntity<byte[]> exportConcept(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(defaultValue = "skos") String format
    ) throws java.io.IOException {
        int userId = conceptAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        conceptAuthSupport.requireThesaurusContributor(userId, thesaurusId);
        var result = conceptSkosExportService.exportConcept(thesaurusId, conceptId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    private String resolveLanguage(String thesaurusId, String lang) {
        return StringUtils.isNotBlank(lang) ? lang : thesaurusWorkLanguageService.resolveForThesaurus(thesaurusId);
    }
}
