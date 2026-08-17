package fr.cnrs.opentheso.v2.preview.api;

import fr.cnrs.opentheso.v2.candidat.model.CandidatStatusCode;
import fr.cnrs.opentheso.v2.setting.ui.ThesaurusContext;
import fr.cnrs.opentheso.v2.shared.repository.ThesaurusHomeQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/v2-preview/api")
@RequiredArgsConstructor
public class PreviewStatsApiController {

    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yy", Locale.FRANCE);

    private final ThesaurusHomeQueryRepository thesaurusHomeQueryRepository;
    private final ThesaurusContext thesaurusContext;

    @GetMapping(value = "/stats/language-coverage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> languageCoverage() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String workLang = StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
        List<Map<String, Object>> languages = thesaurusHomeQueryRepository
                .findLanguageTranslationCoverage(thesaurusId, workLang)
                .stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("code", row.code());
                    item.put("label", capitalize(row.label()));
                    item.put("translatedCount", row.translatedCount());
                    return item;
                })
                .toList();
        return Map.of("languages", languages);
    }

    @GetMapping(value = "/stats/collection-coverage", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> collectionCoverage() {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        String workLang = StringUtils.defaultIfBlank(thesaurusContext.resolveWorkLanguage(), "fr");
        List<Map<String, Object>> collections = thesaurusHomeQueryRepository
                .findCollectionMemberCoverage(thesaurusId, workLang)
                .stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", row.id());
                    item.put("label", row.label());
                    item.put("memberCount", row.memberCount());
                    return item;
                })
                .toList();
        return Map.of("collections", collections);
    }

    @GetMapping(value = "/stats/candidate-life", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> candidateLife() {
        var stats = thesaurusHomeQueryRepository.findCandidateLifeStats(thesaurusContext.resolveThesaurusId());
        NumberFormat integers = NumberFormat.getIntegerInstance(Locale.FRANCE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pending", metric(stats.pending(), integers.format(stats.pending())));
        body.put("accepted", metric(stats.accepted(), integers.format(stats.accepted())));
        body.put("rejected", metric(stats.rejected(), integers.format(stats.rejected())));
        body.put("accepted12m", metric(stats.acceptedLast12Months(), integers.format(stats.acceptedLast12Months())));
        body.put("rejected12m", metric(stats.rejectedLast12Months(), integers.format(stats.rejectedLast12Months())));
        body.put("acceptanceRate", metric(stats.acceptanceRatePercent(), integers.format(stats.acceptanceRatePercent()) + "\u202f%"));
        body.put("medianDecisionDays", stats.medianDecisionDays() == null
                ? metric(null, "—")
                : metric(stats.medianDecisionDays(), integers.format(stats.medianDecisionDays()) + "\u202fj"));
        body.put("activeContributors", metric(stats.activeContributors(), integers.format(stats.activeContributors())));
        return body;
    }

    @GetMapping(value = "/stats/candidate-months", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> candidateMonths() {
        List<Map<String, Object>> months = thesaurusHomeQueryRepository
                .findCandidateMonthlyProposals(thesaurusContext.resolveThesaurusId())
                .stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("key", row.month().toString());
                    item.put("label", monthLabel(row.month()));
                    item.put("accepted", row.accepted());
                    item.put("pending", row.pending());
                    item.put("rejected", row.rejected());
                    item.put("total", row.total());
                    return item;
                })
                .toList();
        return Map.of("months", months);
    }

    @GetMapping(value = "/stats/{metric}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> stat(@PathVariable String metric) {
        String thesaurusId = thesaurusContext.resolveThesaurusId();
        int value = switch (metric) {
            case "concepts" -> thesaurusHomeQueryRepository.countValidConcepts(thesaurusId);
            case "candidates" -> countCandidates(thesaurusId);
            case "collections" -> thesaurusHomeQueryRepository.countCollections(thesaurusId);
            case "languages" -> thesaurusHomeQueryRepository.countDefinedLanguages(thesaurusId);
            case "max-depth" -> thesaurusHomeQueryRepository.findMaxTreeDepth(thesaurusId);
            case "without-definition" -> thesaurusHomeQueryRepository.countConceptsWithoutDefinition(thesaurusId);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
        return Map.of(
                "metric", metric,
                "value", value,
                "formatted", NumberFormat.getIntegerInstance(Locale.FRANCE).format(value)
        );
    }

    private int countCandidates(String thesaurusId) {
        if (StringUtils.isBlank(thesaurusId)) {
            return 0;
        }
        return thesaurusHomeQueryRepository.countCandidatesByStatus(thesaurusId, CandidatStatusCode.PENDING)
                + thesaurusHomeQueryRepository.countCandidatesByStatus(thesaurusId, CandidatStatusCode.REJECTED);
    }

    private static String monthLabel(YearMonth month) {
        return month.format(MONTH_LABEL)
                .replace('\u00a0', ' ')
                .replace('\u202f', ' ')
                .trim();
    }

    private static String capitalize(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private static Map<String, Object> metric(Integer value, String formatted) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("value", value == null ? 0 : value);
        item.put("formatted", formatted);
        return item;
    }
}
