package fr.cnrs.opentheso.v2.candidat.api;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.v2.candidat.api.dto.CandidateSummaryResponse;
import fr.cnrs.opentheso.v2.candidat.api.dto.ExportCandidatesRequest;
import fr.cnrs.opentheso.v2.candidat.api.mapper.CandidatApiMapper;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController("v2CandidatController")
@RequestMapping("/openapi/v2/thesauri/{thesaurusId}/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidats", description = "Consultation et export des candidats (v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class CandidatControllerV2 {

    private final CandidatAuthSupport candidatAuthSupport;
    private final CandidatReadService candidatReadService;
    private final CandidatExportService candidatExportService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les candidats par statut")
    public List<CandidateSummaryResponse> listCandidates(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String search
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String resolvedLang = lang == null || lang.isBlank() ? defaultWorkLanguage : lang;
        int statusId = resolveStatus(status);
        List<CandidatDto> candidates = candidatReadService.searchByStatus(
                thesaurusId, resolvedLang, statusId, search);
        return CandidatApiMapper.toSummaries(candidates);
    }

    @GetMapping(value = "/{conceptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lire le détail d'un candidat")
    public CandidateSummaryResponse getCandidate(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(required = false) String lang
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String resolvedLang = lang == null || lang.isBlank() ? defaultWorkLanguage : lang;
        int statusId = resolveStatus(status);
        CandidatDto candidat = candidatReadService.searchByStatus(
                        thesaurusId, resolvedLang, statusId, null).stream()
                .filter(item -> conceptId.equals(item.getIdConcepte()))
                .findFirst()
                .orElseThrow(() -> new fr.cnrs.opentheso.v2.candidat.exception.CandidateNotFoundException(conceptId));
        candidatReadService.loadDetails(candidat, thesaurusId);
        return CandidatApiMapper.toSummary(candidat);
    }

    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exporter les candidats en attente")
    public ResponseEntity<byte[]> exportPendingCandidates(
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "API-KEY", required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam(required = false) String lang,
            @Valid @RequestBody ExportCandidatesRequest request
    ) throws IOException {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String resolvedLang = lang == null || lang.isBlank() ? defaultWorkLanguage : lang;
        List<CandidatDto> candidates = candidatReadService.loadByStatus(
                thesaurusId, resolvedLang, CandidatStatusCode.PENDING);
        var result = candidatExportService.exportPendingCandidates(
                thesaurusId, candidates, request.format(), ignored -> { });
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    private static int resolveStatus(String status) {
        return switch (status.toLowerCase()) {
            case "accepted" -> CandidatStatusCode.ACCEPTED;
            case "rejected" -> CandidatStatusCode.REJECTED;
            default -> CandidatStatusCode.PENDING;
        };
    }
}
