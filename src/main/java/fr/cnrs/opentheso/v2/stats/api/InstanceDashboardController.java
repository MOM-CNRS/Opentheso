package fr.cnrs.opentheso.v2.stats.api;

import fr.cnrs.opentheso.v2.admin.policy.SuperAdminAccessPolicy;
import fr.cnrs.opentheso.v2.shared.session.AuthenticatedUserSource;
import fr.cnrs.opentheso.v2.stats.model.ConceptMissingDefinition;
import fr.cnrs.opentheso.v2.stats.model.ConceptToTranslate;
import fr.cnrs.opentheso.v2.stats.model.DashboardPeriod;
import fr.cnrs.opentheso.v2.stats.model.InstanceDashboardFilters;
import fr.cnrs.opentheso.v2.stats.model.InstanceDashboardOverview;
import fr.cnrs.opentheso.v2.stats.model.QualityDrawerPage;
import fr.cnrs.opentheso.v2.stats.model.ThesaurusQualitySnapshot;
import fr.cnrs.opentheso.v2.stats.service.InstanceDashboardService;
import fr.cnrs.opentheso.v2.stats.service.ThesaurusQualityService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/v2/api/instance-stats", "/v2-preview/api/instance-stats"})
@RequiredArgsConstructor
public class InstanceDashboardController {

    private final InstanceDashboardService instanceDashboardService;
    private final ThesaurusQualityService thesaurusQualityService;
    private final SuperAdminAccessPolicy superAdminAccessPolicy;
    private final AuthenticatedUserSource authenticatedUserSource;

    @GetMapping(value = "/filters", produces = MediaType.APPLICATION_JSON_VALUE)
    public InstanceDashboardFilters filters() {
        requireAdmin();
        return instanceDashboardService.filters();
    }

    @GetMapping(value = "/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public InstanceDashboardOverview overview(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String thesaurusId
    ) {
        requireAdmin();
        return instanceDashboardService.overview(DashboardPeriod.fromDays(days), thesaurusId);
    }

    @GetMapping(value = "/quality", produces = MediaType.APPLICATION_JSON_VALUE)
    public ThesaurusQualitySnapshot quality(@RequestParam String thesaurusId) {
        requireAdmin();
        requireThesaurus(thesaurusId);
        return thesaurusQualityService.load(thesaurusId.trim());
    }

    @GetMapping(value = "/quality/to-translate", produces = MediaType.APPLICATION_JSON_VALUE)
    public QualityDrawerPage<ConceptToTranslate> toTranslate(
            @RequestParam String thesaurusId,
            @RequestParam int languages
    ) {
        requireAdmin();
        requireThesaurus(thesaurusId);
        return thesaurusQualityService.conceptsToTranslate(thesaurusId.trim(), languages);
    }

    @GetMapping(value = "/quality/missing-definitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public QualityDrawerPage<ConceptMissingDefinition> missingDefinitions(
            @RequestParam String thesaurusId,
            @RequestParam String lang
    ) {
        requireAdmin();
        requireThesaurus(thesaurusId);
        return thesaurusQualityService.missingDefinitions(thesaurusId.trim(), lang);
    }

    private void requireAdmin() {
        superAdminAccessPolicy.requireSuperAdmin(authenticatedUserSource.getUserId().orElse(null));
    }

    private static void requireThesaurus(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            throw new IllegalArgumentException("thesaurusId is required");
        }
    }
}
