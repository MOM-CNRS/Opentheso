package fr.cnrs.opentheso.v2.candidat.api;

import fr.cnrs.opentheso.models.candidats.CandidatDto;
import fr.cnrs.opentheso.models.candidats.enumeration.VoteType;
import fr.cnrs.opentheso.v2.candidat.api.dto.CandidateSummaryResponse;
import fr.cnrs.opentheso.v2.candidat.api.dto.CreateCandidateRequest;
import fr.cnrs.opentheso.v2.candidat.api.dto.ExportCandidatesRequest;
import fr.cnrs.opentheso.v2.candidat.api.dto.ProcessCandidateRequest;
import fr.cnrs.opentheso.v2.candidat.api.dto.VoteCandidateRequest;
import fr.cnrs.opentheso.v2.candidat.api.mapper.CandidatApiMapper;
import fr.cnrs.opentheso.v2.candidat.exception.CandidateNotFoundException;
import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.candidat.service.CandidatExportService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatMutationService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatProcessService;
import fr.cnrs.opentheso.v2.candidat.service.CandidatReadService;
import fr.cnrs.opentheso.v2.shared.api.ApiHeaders;
import fr.cnrs.opentheso.v2.shared.session.ThesaurusPreferencesProvider;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController("v2CandidatController")
@RequestMapping("/openapi/v2/thesauri/{thesaurusId}/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidats", description = "Consultation, création, votes et traitement des candidats (v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class CandidatControllerV2 {

    private final CandidatAuthSupport candidatAuthSupport;
    private final CandidatReadService candidatReadService;
    private final CandidatMutationService candidatMutationService;
    private final CandidatProcessService candidatProcessService;
    private final CandidatExportService candidatExportService;
    private final ThesaurusPreferencesProvider thesaurusPreferencesProvider;
    private final UserProfileService userProfileService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les candidats par statut")
    public List<CandidateSummaryResponse> listCandidates(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam(defaultValue = "pending") String status,
            @RequestParam(required = false) String lang,
            @RequestParam(required = false) String search
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String resolvedLang = resolveLang(lang);
        int statusId = CandidatApiMapper.toStatusCode(status);
        List<CandidatDto> candidates = candidatReadService.searchByStatus(
                thesaurusId, resolvedLang, statusId, search);
        return CandidatApiMapper.toSummaries(candidates);
    }

    @GetMapping(value = "/{conceptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lire le détail d'un candidat")
    public CandidateSummaryResponse getCandidate(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String resolvedLang = resolveLang(lang);
        CandidatDto candidat = candidatReadService.findByConceptId(
                        thesaurusId, conceptId, resolvedLang, userId)
                .orElseThrow(() -> new CandidateNotFoundException(conceptId));
        return CandidatApiMapper.toSummary(candidat);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Créer un candidat")
    public CandidateSummaryResponse createCandidate(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @Valid @RequestBody CreateCandidateRequest request
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String lang = resolveLang(request.lang());
        String username = resolveUsername(userId);

        CandidatDto candidat = new CandidatDto();
        candidat.setNomPref(request.preferredLabel().trim());
        candidat.setIdThesaurus(thesaurusId);
        candidat.setLang(lang);

        String definition = StringUtils.defaultString(request.definition());
        if (!candidatMutationService.saveNewCandidat(
                candidat, thesaurusId, lang, userId, username, lang, definition)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create candidate");
        }
        candidatMutationService.saveContributorMetadata(candidat.getIdConcepte(), thesaurusId, username);
        return CandidatApiMapper.toSummary(
                candidatReadService.findByConceptId(thesaurusId, candidat.getIdConcepte(), lang, userId)
                        .orElse(candidat));
    }

    @PostMapping(value = "/{conceptId}/votes", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Voter pour ou contre un candidat")
    public CandidateSummaryResponse voteCandidate(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang,
            @Valid @RequestBody VoteCandidateRequest request
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String resolvedLang = resolveLang(lang);
        CandidatDto candidat = candidatReadService.findByConceptId(
                        thesaurusId, conceptId, resolvedLang, userId)
                .orElseThrow(() -> new CandidateNotFoundException(conceptId));

        VoteType type = "down".equalsIgnoreCase(request.direction()) ? VoteType.CONTRE : VoteType.CANDIDAT;
        VoteType opposite = type == VoteType.CONTRE ? VoteType.CANDIDAT : VoteType.CONTRE;
        if (candidatMutationService.hasVote(thesaurusId, conceptId, userId, null, opposite)) {
            candidatMutationService.removeVote(thesaurusId, conceptId, userId, null, opposite);
        }
        if (candidatMutationService.hasVote(thesaurusId, conceptId, userId, null, type)) {
            candidatMutationService.removeVote(thesaurusId, conceptId, userId, null, type);
        } else {
            candidatMutationService.addVote(thesaurusId, conceptId, userId, null, type);
        }
        return CandidatApiMapper.toSummary(
                candidatReadService.findByConceptId(thesaurusId, conceptId, resolvedLang, userId)
                        .orElse(candidat));
    }

    @PostMapping(value = "/{conceptId}/process", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Accepter ou rejeter un candidat")
    public CandidateSummaryResponse processCandidate(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang,
            @Valid @RequestBody ProcessCandidateRequest request
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireManager(userId, thesaurusId);
        String resolvedLang = resolveLang(lang);
        CandidatDto candidat = candidatReadService.findByConceptId(
                        thesaurusId, conceptId, resolvedLang, userId)
                .orElseThrow(() -> new CandidateNotFoundException(conceptId));
        if (candidat.getCreatedById() > 0 && candidat.getCreatedById() == userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot process own candidate");
        }

        String adminMessage = StringUtils.defaultString(request.message());
        String username = resolveUsername(userId);
        boolean accept = "accept".equalsIgnoreCase(request.action());
        var outcome = accept
                ? candidatProcessService.insertCandidate(candidat, adminMessage, userId)
                : candidatProcessService.rejectCandidate(candidat, adminMessage, userId);
        if (outcome.isFailure()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Process failed");
        }
        if (accept) {
            thesaurusPreferencesProvider.findPreferences(thesaurusId)
                    .ifPresent(prefs -> candidatProcessService.afterCandidateAccepted(
                            candidat, userId, username, prefs));
        } else {
            candidatProcessService.afterCandidateRejected(candidat, userId, username);
        }
        return CandidatApiMapper.toSummary(
                candidatReadService.findByConceptId(thesaurusId, conceptId, resolvedLang, userId)
                        .orElse(candidat));
    }

    @PostMapping(value = "/{conceptId}/reactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Réactiver un candidat rejeté")
    public CandidateSummaryResponse reactivateCandidate(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable String conceptId,
            @RequestParam(required = false) String lang
    ) {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireManager(userId, thesaurusId);
        String resolvedLang = resolveLang(lang);
        if (candidatReadService.findByConceptId(thesaurusId, conceptId, resolvedLang, userId).isEmpty()) {
            throw new CandidateNotFoundException(conceptId);
        }
        if (!candidatMutationService.updateCandidateStatus(thesaurusId, conceptId, CandidatStatusCode.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reactivate failed");
        }
        return CandidatApiMapper.toSummary(
                candidatReadService.findByConceptId(thesaurusId, conceptId, resolvedLang, userId)
                        .orElseThrow(() -> new CandidateNotFoundException(conceptId)));
    }

    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exporter les candidats en attente")
    public ResponseEntity<byte[]> exportPendingCandidates(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam(required = false) String lang,
            @Valid @RequestBody ExportCandidatesRequest request
    ) throws IOException {
        int userId = candidatAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        candidatAuthSupport.requireContributor(userId, thesaurusId);
        String resolvedLang = resolveLang(lang);
        List<CandidatDto> candidates = candidatReadService.loadByStatus(
                thesaurusId, resolvedLang, CandidatStatusCode.PENDING);
        var result = candidatExportService.exportPendingCandidates(
                thesaurusId, candidates, request.format(), ignored -> { });
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    private String resolveLang(String lang) {
        return lang == null || lang.isBlank() ? defaultWorkLanguage : lang;
    }

    private String resolveUsername(int userId) {
        try {
            return userProfileService.getProfile(userId).username();
        } catch (Exception ex) {
            return "api-user-" + userId;
        }
    }
}
