package fr.cnrs.opentheso.toolbox.api;

import fr.cnrs.opentheso.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.toolbox.model.StatisticsCompleteness;
import fr.cnrs.opentheso.toolbox.model.StatisticsKpis;
import fr.cnrs.opentheso.toolbox.model.StatisticsOverview;
import fr.cnrs.opentheso.toolbox.service.ThesaurusStatisticsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StatsApiController {

    private final ThesaurusStatisticsService thesaurusStatisticsService;
    private final ThesaurusContext thesaurusContext;

    @GetMapping(value = "/stats/kpis", produces = MediaType.APPLICATION_JSON_VALUE)
    public StatisticsKpis kpis() {
        return thesaurusStatisticsService.loadKpis(thesaurusId());
    }

    @GetMapping(value = "/stats/overview", produces = MediaType.APPLICATION_JSON_VALUE)
    public StatisticsOverview overview() {
        return thesaurusStatisticsService.loadOverview(thesaurusId(), workLang());
    }

    @GetMapping(value = "/stats/completeness", produces = MediaType.APPLICATION_JSON_VALUE)
    public StatisticsCompleteness completeness() {
        return thesaurusStatisticsService.loadCompleteness(thesaurusId());
    }

    private String thesaurusId() {
        return thesaurusContext.resolveThesaurusId();
    }

    private String workLang() {
        return StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
    }
}
