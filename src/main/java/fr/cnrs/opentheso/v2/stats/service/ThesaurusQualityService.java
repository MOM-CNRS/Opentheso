package fr.cnrs.opentheso.v2.stats.service;

import fr.cnrs.opentheso.v2.setting.model.ThesaurusPreferences;
import fr.cnrs.opentheso.v2.setting.service.ThesaurusPreferenceService;
import fr.cnrs.opentheso.v2.stats.model.ConceptMissingDefinition;
import fr.cnrs.opentheso.v2.stats.model.ConceptToTranslate;
import fr.cnrs.opentheso.v2.stats.model.DefinitionCoverage;
import fr.cnrs.opentheso.v2.stats.model.LanguageCoverageBucket;
import fr.cnrs.opentheso.v2.stats.model.QualityCriterion;
import fr.cnrs.opentheso.v2.stats.model.QualityDrawerPage;
import fr.cnrs.opentheso.v2.stats.model.QualityScore;
import fr.cnrs.opentheso.v2.stats.model.ThesaurusQualitySnapshot;
import fr.cnrs.opentheso.v2.stats.persistence.ThesaurusQualityQueryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesaurusQualityService {

    private static final Logger log = LoggerFactory.getLogger(ThesaurusQualityService.class);

    public static final double WEIGHT_DEFINITION = 0.15;
    public static final double WEIGHT_TRANSLATED_TERMS = 0.10;
    public static final double WEIGHT_TRANSLATED_DEFS = 0.10;
    public static final double WEIGHT_ALIGNMENT = 0.10;
    public static final double WEIGHT_ARK = 0.10;
    public static final double WEIGHT_RT = 0.10;
    public static final double WEIGHT_HIERARCHY = 0.20;
    public static final double WEIGHT_FRESHNESS = 0.15;
    public static final int DRAWER_LIMIT = 200;

    private final ThesaurusQualityQueryRepository qualityQueryRepository;
    private final ThesaurusPreferenceService thesaurusPreferenceService;

    @Transactional(readOnly = true)
    public ThesaurusQualitySnapshot load(String thesaurusId) {
        try {
            return doLoad(thesaurusId);
        } catch (DataAccessException e) {
            log.warn("Qualité du thésaurus {} indisponible : {}", thesaurusId, e.getMessage());
            String sourceLang = resolveSourceLang(thesaurusId);
            return new ThesaurusQualitySnapshot(
                    thesaurusId,
                    new QualityScore(0, QualityScore.zoneFor(0), sourceLang, List.of()),
                    List.of(), List.of(), 0, List.of()
            );
        }
    }

    private ThesaurusQualitySnapshot doLoad(String thesaurusId) {
        String sourceLang = resolveSourceLang(thesaurusId);
        List<QualityCriterion> criteria = List.of(
                new QualityCriterion("definition", "Concepts avec une définition",
                        qualityQueryRepository.definitionCoverage(thesaurusId, sourceLang), WEIGHT_DEFINITION),
                new QualityCriterion("translatedTerms", "Termes traduits",
                        qualityQueryRepository.translatedTermsCoverage(thesaurusId, sourceLang), WEIGHT_TRANSLATED_TERMS),
                new QualityCriterion("translatedDefs", "Définitions traduites",
                        qualityQueryRepository.translatedDefinitionsCoverage(thesaurusId, sourceLang), WEIGHT_TRANSLATED_DEFS),
                new QualityCriterion("alignments", "Concepts alignés",
                        qualityQueryRepository.alignmentCoverage(thesaurusId), WEIGHT_ALIGNMENT),
                new QualityCriterion("ark", "Identifiants ARK",
                        qualityQueryRepository.arkCoverage(thesaurusId), WEIGHT_ARK),
                new QualityCriterion("rt", "Relations associatives (RT)",
                        qualityQueryRepository.rtCoverage(thesaurusId), WEIGHT_RT),
                new QualityCriterion("hierarchy", "Relations hiérarchiques (BT/NT)",
                        qualityQueryRepository.hierarchyCoverage(thesaurusId), WEIGHT_HIERARCHY),
                new QualityCriterion("freshness", "Fraîcheur du thésaurus",
                        freshnessScore(qualityQueryRepository.lastModification(thesaurusId)), WEIGHT_FRESHNESS)
        );
        double overall = criteria.stream().mapToDouble(QualityCriterion::contribution).sum();
        List<LanguageCoverageBucket> coverage = qualityQueryRepository.languageCoverage(thesaurusId);
        return new ThesaurusQualitySnapshot(
                thesaurusId,
                new QualityScore(round2(overall), QualityScore.zoneFor(overall), sourceLang, criteria),
                qualityQueryRepository.thesaurusLanguages(thesaurusId),
                coverage,
                averageLanguages(coverage),
                qualityQueryRepository.definitionCoverageByLanguage(thesaurusId)
        );
    }

    @Transactional(readOnly = true)
    public QualityDrawerPage<ConceptToTranslate> conceptsToTranslate(String thesaurusId, int languageCount) {
        try {
            long total = qualityQueryRepository.countConceptsByLanguageCount(thesaurusId, languageCount);
            return new QualityDrawerPage<>(
                    total,
                    qualityQueryRepository.conceptsByLanguageCount(
                            thesaurusId, languageCount, resolveSourceLang(thesaurusId), DRAWER_LIMIT)
            );
        } catch (DataAccessException e) {
            log.warn("Liste à traduire {} indisponible : {}", thesaurusId, e.getMessage());
            return QualityDrawerPage.empty();
        }
    }

    @Transactional(readOnly = true)
    public QualityDrawerPage<ConceptMissingDefinition> missingDefinitions(String thesaurusId, String lang) {
        try {
            long total = qualityQueryRepository.countMissingDefinitions(thesaurusId, lang);
            return new QualityDrawerPage<>(
                    total,
                    qualityQueryRepository.missingDefinitions(
                            thesaurusId, lang, resolveSourceLang(thesaurusId), DRAWER_LIMIT)
            );
        } catch (DataAccessException e) {
            log.warn("Liste sans définition {} indisponible : {}", thesaurusId, e.getMessage());
            return QualityDrawerPage.empty();
        }
    }

    static double freshnessScore(LocalDateTime lastModification) {
        if (lastModification == null) {
            return 0;
        }
        long months = ChronoUnit.MONTHS.between(lastModification.toLocalDate(), LocalDate.now());
        if (months < 3) {
            return 100;
        }
        if (months < 6) {
            return 95;
        }
        if (months < 12) {
            return 85;
        }
        if (months < 24) {
            return 70;
        }
        if (months < 36) {
            return 50;
        }
        if (months < 60) {
            return 30;
        }
        return 0;
    }

    private String resolveSourceLang(String thesaurusId) {
        ThesaurusPreferences preferences = thesaurusPreferenceService.loadPreferencesOrNull(thesaurusId, "fr");
        if (preferences != null && StringUtils.isNotBlank(preferences.sourceLang())) {
            return preferences.sourceLang().trim();
        }
        return "fr";
    }

    private static double averageLanguages(List<LanguageCoverageBucket> coverage) {
        long total = 0;
        long weighted = 0;
        for (LanguageCoverageBucket bucket : coverage) {
            total += bucket.conceptCount();
            weighted += (long) bucket.languageCount() * bucket.conceptCount();
        }
        if (total == 0) {
            return 0;
        }
        return Math.round(weighted * 100.0 / total) / 100.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
