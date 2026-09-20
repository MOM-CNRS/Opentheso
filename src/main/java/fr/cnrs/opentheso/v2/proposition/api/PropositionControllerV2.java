package fr.cnrs.opentheso.v2.proposition.api;

import fr.cnrs.opentheso.v2.proposition.api.dto.PendingCountResponse;
import fr.cnrs.opentheso.v2.proposition.api.dto.ProcessPropositionRequest;
import fr.cnrs.opentheso.v2.proposition.api.dto.PropositionDetailResponse;
import fr.cnrs.opentheso.v2.proposition.api.dto.PropositionSummaryResponse;
import fr.cnrs.opentheso.v2.proposition.api.dto.SubmitPropositionRequest;
import fr.cnrs.opentheso.v2.proposition.api.mapper.PropositionApiMapper;
import fr.cnrs.opentheso.v2.proposition.exception.PropositionNotFoundException;
import fr.cnrs.opentheso.v2.proposition.model.PropositionAcceptance;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraft;
import fr.cnrs.opentheso.v2.proposition.model.PropositionDraftMapper;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSubmission;
import fr.cnrs.opentheso.v2.proposition.model.PropositionSummary;
import fr.cnrs.opentheso.v2.proposition.service.PropositionDraftService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionMutationService;
import fr.cnrs.opentheso.v2.proposition.service.PropositionReadService;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.shared.api.ApiHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController("v2PropositionController")
@RequestMapping("/openapi/v2/thesauri/{thesaurusId}/propositions")
@RequiredArgsConstructor
@Tag(name = "Propositions", description = "Suggestions de modification sur concepts publiés (v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class PropositionControllerV2 {

    private final PropositionAuthSupport propositionAuthSupport;
    private final PropositionReadService propositionReadService;
    private final PropositionMutationService propositionMutationService;
    private final PropositionDraftService propositionDraftService;
    private final ThesaurusPreferenceService thesaurusPreferenceService;

    @Value("${settings.workLanguage:fr}")
    private String defaultWorkLanguage;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les propositions")
    public List<PropositionSummaryResponse> listPropositions(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @RequestParam(defaultValue = "pending") String status
    ) {
        int userId = propositionAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        propositionAuthSupport.requireBoard(userId, thesaurusId);
        List<PropositionSummary> summaries = PropositionApiMapper.isPendingFilter(status)
                ? propositionReadService.listPending(thesaurusId)
                : propositionReadService.listAll(thesaurusId);
        return PropositionApiMapper.toSummaries(summaries);
    }

    @GetMapping(value = "/pending-count", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Compteur de propositions en attente")
    public PendingCountResponse pendingCount(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId
    ) {
        int userId = propositionAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        propositionAuthSupport.requireBoard(userId, thesaurusId);
        return new PendingCountResponse(propositionReadService.countPending(thesaurusId));
    }

    @GetMapping(value = "/{propositionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lire le détail d'une proposition")
    public PropositionDetailResponse getProposition(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable int propositionId,
            @RequestParam(defaultValue = "true") boolean markRead
    ) {
        int userId = propositionAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        propositionAuthSupport.requireBoard(userId, thesaurusId);
        var detail = requireDetailInThesaurus(thesaurusId, propositionId);
        if (markRead) {
            propositionMutationService.markRead(propositionId);
            detail = requireDetailInThesaurus(thesaurusId, propositionId);
        }
        return PropositionApiMapper.toDetail(detail);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Soumettre une proposition")
    public PropositionDetailResponse submitProposition(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @Valid @RequestBody SubmitPropositionRequest request
    ) {
        int userId = propositionAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        String lang = resolveLang(request.lang());
        propositionAuthSupport.requireSubmit(thesaurusId, lang);

        String authorName = StringUtils.defaultIfBlank(
                request.authorName(), propositionAuthSupport.resolveUsername(userId));
        String authorEmail = StringUtils.defaultIfBlank(
                request.authorEmail(), propositionAuthSupport.resolveEmail(userId));
        String thesaurusTitle = resolveThesaurusTitle(thesaurusId, lang);

        var submission = new PropositionSubmission(
                thesaurusId,
                thesaurusTitle,
                request.conceptId().trim(),
                StringUtils.defaultString(request.conceptLabel()),
                lang,
                authorName,
                StringUtils.defaultString(authorEmail),
                request.comment().trim(),
                request.allowMultiplePending() == null || Boolean.TRUE.equals(request.allowMultiplePending())
        );

        Integer propositionId = propositionMutationService.submitDraft(submission)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Unable to create proposition"));

        PropositionDraft draft = PropositionDraftMapper.toDraft(new PropositionDraftMapper.ToDraftRequest(
                request.conceptId().trim(),
                thesaurusId,
                lang,
                request.currentPreferredLabel(),
                request.proposedPreferredLabel(),
                null,
                null,
                null
        ));
        if (!draft.isEmpty()) {
            propositionDraftService.saveDraftDetails(propositionId, draft);
        }

        return PropositionApiMapper.toDetail(requireDetailInThesaurus(thesaurusId, propositionId));
    }

    @PostMapping(value = "/{propositionId}/process",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Approuver ou refuser une proposition")
    public PropositionDetailResponse processProposition(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable int propositionId,
            @Valid @RequestBody ProcessPropositionRequest request
    ) {
        int userId = propositionAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var detail = requireDetailInThesaurus(thesaurusId, propositionId);
        propositionAuthSupport.requireDecide(userId, thesaurusId, detail.authorEmail());

        String reviewerName = propositionAuthSupport.resolveUsername(userId);
        String adminMessage = StringUtils.defaultString(request.message());
        String thesaurusTitle = resolveThesaurusTitle(thesaurusId, detail.lang());
        boolean approve = "approve".equalsIgnoreCase(request.action())
                || "accept".equalsIgnoreCase(request.action());

        if (approve) {
            PropositionDraft draft = propositionDraftService.loadDraftChanges(propositionId);
            if (draft != null && !draft.isEmpty()) {
                var errors = propositionDraftService.applyAcceptedChanges(
                        draft,
                        detail.thesaurusId(),
                        detail.conceptId(),
                        detail.lang(),
                        userId,
                        reviewerName,
                        PropositionAcceptance.all()
                );
                if (!errors.isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, String.join("; ", errors));
                }
            }
            propositionMutationService.approve(
                    propositionId, reviewerName, adminMessage, detail.conceptLabel(), thesaurusTitle);
        } else if ("refuse".equalsIgnoreCase(request.action())
                || "reject".equalsIgnoreCase(request.action())) {
            propositionMutationService.refuse(
                    propositionId, reviewerName, adminMessage, detail.conceptLabel(), thesaurusTitle);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown action");
        }

        return PropositionApiMapper.toDetail(requireDetailInThesaurus(thesaurusId, propositionId));
    }

    @DeleteMapping(value = "/{propositionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une proposition")
    public void deleteProposition(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId,
            @PathVariable int propositionId
    ) {
        int userId = propositionAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        var detail = requireDetailInThesaurus(thesaurusId, propositionId);
        propositionAuthSupport.requireDecide(userId, thesaurusId, detail.authorEmail());
        propositionMutationService.delete(propositionId);
    }

    private fr.cnrs.opentheso.v2.proposition.model.PropositionDetail requireDetailInThesaurus(
            String thesaurusId, int propositionId) {
        var detail = propositionReadService.findDetail(propositionId);
        if (detail == null || !thesaurusId.equals(detail.thesaurusId())) {
            throw new PropositionNotFoundException(propositionId);
        }
        return detail;
    }

    private String resolveLang(String lang) {
        return lang == null || lang.isBlank() ? defaultWorkLanguage : lang;
    }

    private String resolveThesaurusTitle(String thesaurusId, String lang) {
        var preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, resolveLang(lang));
        if (preferences != null && StringUtils.isNotBlank(preferences.preferredName())) {
            return preferences.preferredName();
        }
        return thesaurusId;
    }

}
