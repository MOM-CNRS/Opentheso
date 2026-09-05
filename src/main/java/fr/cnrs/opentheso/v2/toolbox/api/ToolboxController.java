package fr.cnrs.opentheso.v2.toolbox.api;

import fr.cnrs.opentheso.v2.toolbox.api.dto.EditionStatisticsResponse;
import fr.cnrs.opentheso.v2.toolbox.api.dto.EditionThesaurusResponse;
import fr.cnrs.opentheso.v2.toolbox.api.dto.StatisticsSummaryResponse;
import fr.cnrs.opentheso.v2.toolbox.api.mapper.ToolboxApiMapper;
import fr.cnrs.opentheso.v2.toolbox.service.EditionThesaurusService;
import fr.cnrs.opentheso.v2.toolbox.service.ThesaurusStatisticsService;
import fr.cnrs.opentheso.v2.user.model.UserProfile;
import fr.cnrs.opentheso.v2.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import fr.cnrs.opentheso.v2.shared.api.ApiHeaders;

@RestController
@RequestMapping("/openapi/v2/toolbox")
@RequiredArgsConstructor
@Tag(name = "Boîte à outils", description = "Édition et statistiques des thésaurus (v2)")
@SecurityRequirement(name = "ApiKeyAuth")
public class ToolboxController {

    private final ToolboxAuthSupport toolboxAuthSupport;
    private final UserProfileService userProfileService;
    private final EditionThesaurusService editionThesaurusService;
    private final ThesaurusStatisticsService thesaurusStatisticsService;

    @GetMapping(value = "/edition/thesauri", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les thésaurus administrables")
    public List<EditionThesaurusResponse> listEditionThesauri(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey
    ) {
        int userId = toolboxAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        toolboxAuthSupport.requireEditionAccess(userId);
        UserProfile profile = userProfileService.getProfile(userId);
        return ToolboxApiMapper.toThesaurusResponses(
                editionThesaurusService.listAdminThesauri(userId, profile.superAdmin())
        );
    }

    @GetMapping(value = "/edition/{thesaurusId}/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Statistiques d'édition d'un thésaurus")
    public EditionStatisticsResponse editionStatistics(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId
    ) {
        int userId = toolboxAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        toolboxAuthSupport.requireEditionAccess(userId);
        return ToolboxApiMapper.toStatisticsResponse(editionThesaurusService.loadStatistics(thesaurusId));
    }

    @GetMapping(value = "/statistics/{thesaurusId}/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Résumé statistique d'un thésaurus")
    public StatisticsSummaryResponse statisticsSummary(
            @RequestHeader(value = ApiHeaders.X_API_KEY, required = false) String xApiKey,
            @RequestHeader(value = ApiHeaders.API_KEY, required = false) String legacyApiKey,
            @PathVariable String thesaurusId
    ) {
        int userId = toolboxAuthSupport.resolveUserId(xApiKey, legacyApiKey);
        toolboxAuthSupport.requireStatisticsAccess(userId);
        return ToolboxApiMapper.toSummaryResponse(thesaurusStatisticsService.loadSummary(thesaurusId));
    }
}
